/* -*- java -*-
# =========================================================================== #
#                                                                             #
#                         Copyright (C) KNAPP AG                              #
#                                                                             #
#       The copyright to the computer program(s) herein is the property       #
#       of Knapp.  The program(s) may be used   and/or copied only with       #
#       the  written permission of  Knapp  or in  accordance  with  the       #
#       terms and conditions stipulated in the agreement/contract under       #
#       which the program(s) have been supplied.                              #
#                                                                             #
# =========================================================================== #
*/

package com.knapp.codingcontest.data;

import java.util.Collections;
import java.util.Set;

public class Shelf {
  private final String code;
  private final Position position;
  private final Set<String> products;

  // ----------------------------------------------------------------------------

  public Shelf(final String code, final Position position, final Set<String> products) {
    this.code = code;
    this.position = position;
    this.products = products;
  }

  // ----------------------------------------------------------------------------

  public String getCode() {
    return code;
  }

  public Position getPosition() {
    return position;
  }

  public Set<String> getProducts() {
    return Collections.unmodifiableSet(products);
  }

  @Override
  public String toString() {
    return "Shelf#" + code + "@" + position;
  }

  // ----------------------------------------------------------------------------
  // ----------------------------------------------------------------------------
}
