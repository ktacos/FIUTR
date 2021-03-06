/* -*- indent-tabs-mode: nil; c-basic-offset: 2; tab-width: 2 -*-  */
/*
 * network.h
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
 
#ifndef GROUPGD_NETWORK_H_
#define GROUPGD_NETWORK_H_

#include <string>

namespace groupgd {

/**
 * Two networks within this range of each other are considered identical.
 */
const int IDENTICAL_NETWORK_METERS = 40;

/**
 * Stores all fields of a network. Strings are used to maintain precision.
 */
struct Network
{
  std::string name;
  std::string lat;
  std::string lon;
  std::string strength;
};

bool
operator==(Network n1, Network n2);

bool operator!=(Network n1, Network n2);

float
distance(Network n1, Network n2);

}

#endif
