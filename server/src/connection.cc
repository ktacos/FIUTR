/* -*- indent-tabs-mode: nil; c-basic-offset: 2; tab-width: 2 -*-  */
/*
 * connection.cc
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

#include "connection.h"

#include <functional>

#include <boost/asio.hpp>
#include <boost/system/error_code.hpp>
#include <boost/system/system_error.hpp>
#include <systemd/sd-daemon.h>

#include "utility.h"

namespace groupgd {

Connection::Connection(boost::asio::io_service* io_service) noexcept
: deadline_timer_(*io_service), socket_(*io_service)
{ }

Connection::~Connection()
{
  try
  {
    stop();
  }
  catch (boost::system::system_error& e)
  {
    safe_journal(SD_ERR, e.what());
  }
}

void
Connection::async_run()
{
  deadline_timer_.async_wait(std::bind(&Connection::on_deadline_timer_expired,
                                       shared_from_this()));
  async_await_client_query();
}

void
Connection::async_await_client_query()
{
  deadline_timer_.expires_from_now(TIMEOUT);
  boost::asio::async_read_until(socket_, streambuf_, "\r\n",
                                std::bind(&Connection::on_read_completed,
                                          shared_from_this(),
                                          std::placeholders::_1));
}

void
Connection::on_deadline_timer_expired()
{
  // Timed operation could have completed after the timer expired, yet before
  // this function was called. In that case, keep the connection open.
  if (deadline_timer_.expires_at()
        <= boost::asio::deadline_timer::traits_type::now())
    stop();
  else
    deadline_timer_.async_wait(std::bind(&Connection::on_deadline_timer_expired,
                                         shared_from_this()));
}

void
Connection::on_read_completed(const boost::system::error_code& ec)
{
  if (!ec)
    {
      auto query = read_line_from_streambuf(&streambuf_);
      if (query == "GET NETWORKS")
        async_send_networks_to_client();
      else if (query.find("ADD NETWORK") == 0)
        async_add_network_to_database(query);
      else
        safe_journal(SD_WARNING, std::string("Unexpected query: ") + query);
    }
  else if (ec != boost::asio::error::operation_aborted
           && ec != boost::asio::error::eof)
    {
      throw boost::system::system_error{ec};
    }
}

void
Connection::on_write_completed(const boost::system::error_code& ec,
                               std::size_t /*bytes_transferred*/)
{
  if (!ec)
    {
      async_await_client_query();
    }
  else if (ec != boost::asio::error::operation_aborted)
    {
      throw boost::system::system_error{ec};
    }
}

void
Connection::async_add_network_to_database(std::string query)
{
  // TODO implement
  async_await_client_query();
}

void
Connection::async_send_networks_to_client()
{
  // TODO implement
  deadline_timer_.expires_from_now(TIMEOUT);
  auto response = std::string{"I've got no networks for you yet.\r\n"};
  boost::asio::async_write(socket_,
                           boost::asio::buffer(response),
                           std::bind(&Connection::on_write_completed,
                                     shared_from_this(),
                                     std::placeholders::_1,
                                     std::placeholders::_2));
}

void
Connection::stop()
{
  deadline_timer_.cancel();

  if (socket_.is_open())
    {
      socket_.shutdown(boost::asio::ip::tcp::socket::shutdown_both);
      socket_.close();
    }
}

}
