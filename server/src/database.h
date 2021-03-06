/* -*- indent-tabs-mode: nil; c-basic-offset: 2; tab-width: 2 -*-  */
/*
 * database.h
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

#ifndef GROUPGD_DATABASE_H_
#define GROUPGD_DATABASE_H_

#include <memory>
#include <string>
#include <vector>

#include <boost/property_tree/ptree.hpp>

class sqlite3;

namespace groupgd {

struct Network;

/**
 * An object for handling database accesses.
 */
class Database
{
public:
  Database();

  ~Database();

  void
  add_network(const Network& network);

  boost::property_tree::ptree
  all_networks_as_ptree() const;

  std::string
  all_networks_as_string() const;

private:
  Database(const Database&) = delete;

  Database&
  operator=(const Database&) = delete;

  void
  open_database();

  void
  ensure_network_table_exists();

  std::vector<Network>
  networks_with_ssid(std::string name);

  void
  remove_network(const Network& network);

  sqlite3* db_;
};

}

#endif
