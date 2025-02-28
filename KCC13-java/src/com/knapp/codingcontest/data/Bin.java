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

public abstract class Bin {
  private final String code;
  private final Position position;

  // ----------------------------------------------------------------------------

  protected Bin(final String code, final Position position) {
    this.code = code;
    this.position = position;
  }

  // ----------------------------------------------------------------------------

  public String getCode() {
    return code;
  }

  public Position getPosition() {
    return position;
  }

  @Override
  public String toString() {
    return "Bin#" + code + "@" + position;
  }

  // ----------------------------------------------------------------------------
  // ----------------------------------------------------------------------------
}
