/* -*- indent-tabs-mode: nil; c-basic-offset: 2; tab-width: 2 -*-  */
/*
 * connection_manager.cc
 * Copyright (C) 2013 Michael Catanzaro <michael.catanzaro@mst.edu>
 *
 * This file is part of groupgd.
 *
 * groupgd is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * groupgd is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

#include "connection_manager.h"

#include <cassert>
#include <cstdlib>
#include <functional>
#include <memory>
#include <stdexcept>
#include <thread>

#include <boost/asio.hpp>
#include <boost/system/error_code.hpp>
#include <boost/system/system_error.hpp>
#include <systemd/sd-daemon.h>

#include "connection.h"
#include "utility.h"

namespace groupgd {

/**
 * Access the singleton instance of the ConnectionManager.
 */
ConnectionManager&
ConnectionManager::instance()
{
  static ConnectionManager manager;
  return manager;
}

/**
 * Closes the acceptor socket.
 */
ConnectionManager::~ConnectionManager()
{
  acceptor_.close();
}

/**
 * Runs the ConnectionManager. You should only call this once in your program.
 * run() will exit once all clients have disconnected.
 */
void
ConnectionManager::run()
{
  signal_set_.async_wait(std::bind(&ConnectionManager::handle_signal,
                                   this,
                                   std::placeholders::_1,
                                   std::placeholders::_2));

  // The acceptor io_service will run indefinitely due to the async_wait on the
  // signal set, either until a signal is caught or it is explicitly stopped.
  std::thread acceptor_thread{[this]() {
                                 try
                                 {
                                   acceptor_io_service_.run();
                                 }
                                 catch (std::exception& e)
                                 {
                                   connection_io_service_.stop();
                                   exit_status_ = EXIT_FAILURE;
                                   safe_journal(SD_CRIT, e.what());
                                 }
                               }};

  try
  {
    accept_initial_connection();
    async_accept_additional_connections();
    // The connection io_service will run until all clients have disconnected.
    connection_io_service_.run();
  }
  catch (std::exception& e)
  {
    acceptor_io_service_.stop();
    acceptor_thread.join();
    throw;
  }

  // Time to shut down. systemd will restart us when a new client appears.
  acceptor_io_service_.stop();
  acceptor_thread.join();

  if (rethrow_signal_)
    std::raise(rethrow_signal_);
  
  if (exit_status_)
    std::exit(exit_status_);
}

/**
 * Create a new ConnectionManager.
 */
ConnectionManager::ConnectionManager()
: acceptor_(acceptor_io_service_), 
  signal_set_(acceptor_io_service_, SIGHUP, SIGTERM)
{
  if (sd_listen_fds(0) != 1)
    throw std::runtime_error{"Must be socket-activated with one socket"};
  else if (!sd_is_socket_inet(SD_LISTEN_FDS_START, AF_INET, SOCK_STREAM, 1, 0))
    throw std::runtime_error{"Activated with socket of incorrect type"};

  acceptor_.assign(boost::asio::ip::tcp::v4(), SD_LISTEN_FDS_START);
}

/**
 * Synchronously accepts a connection from the acceptor socket. Asynchronously
 * runs the client/server protocol for that connection.
 */
void
ConnectionManager::accept_initial_connection()
{
  auto connection = std::make_shared<Connection>(&connection_io_service_);
  acceptor_.accept(*(connection->mutable_socket()));
  connection->async_run();
}

/**
 * Accepts a connection from the acceptor socket, asynchronously runs the
 * client/server protocol for that connection, and continues listening for
 * more sockets.
 */
void
ConnectionManager::async_accept_additional_connections()
{
  auto connection = std::make_shared<Connection>(&connection_io_service_);
  acceptor_.async_accept(*(connection->mutable_socket()),
                         [=](const boost::system::error_code& ec) {
                           if (!ec)
                             connection->async_run();
                           else if (ec != boost::asio::error::operation_aborted)
                             throw boost::system::system_error{ec};
                           async_accept_additional_connections();
                         });
}

/**
 * Cleanly handles SIGTERM and ignores SIGHUP.
 *
 * @param ec used to indicate an error or cancellation
 * @param signal_number the POSIX signal that has been caught
 */
void
ConnectionManager::handle_signal(const boost::system::error_code& ec, 
                                 int signal_number)
{
  if (!ec)
    {
      switch (signal_number)
        {
        case SIGTERM:
          // When the ConnectionManager is destroyed at the end of main,
          // connection_io_service_'s destructor will destroy all active
          // handlers, and with those the last remaining shared pointers to the
          // remaining connection objects. This ensures all sockets are closed.
          acceptor_io_service_.stop();
          connection_io_service_.stop();
          rethrow_signal_ = SIGTERM;
          break; 
        case SIGHUP:
          // No config files to reload, so ignore this signal.
          break;
        default:
          assert(false);
        }
    }
  else if (ec != boost::asio::error::operation_aborted)
    throw boost::system::system_error{ec};
}

}
