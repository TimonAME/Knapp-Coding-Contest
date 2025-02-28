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

import java.io.Serializable;

public class Position implements Serializable {
  private static final long serialVersionUID = 1L;

  public enum Side {
    Left, Right,;
  }

  public enum Offset {
    Bin, Shelf,;
  }

  // ----------------------------------------------------------------------------

  public final Side side;
  public final Offset offset;
  public final int lengthwise;

  // ----------------------------------------------------------------------------

  public Position(final Side side, final Offset offset, final int lengthwise) {
    this.side = side;
    this.offset = offset;
    this.lengthwise = lengthwise;
  }

  // ----------------------------------------------------------------------------

  @Override
  public String toString() {
    return "Position[" + side + "/" + offset + " @" + lengthwise + "]";
  }

  // ----------------------------------------------------------------------------
}
