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

package com.knapp.codingcontest.operations.ex;

import com.knapp.codingcontest.core.WarehouseInternal.Operation.FinishOrder;

public class OrderNotCompletelyPickedException extends AbstractWarehouseException {
  private static final long serialVersionUID = 1L;

  // ----------------------------------------------------------------------------

  public OrderNotCompletelyPickedException(final FinishOrder op) {
    super(op.toResultString() + "{op=" + op.order.getOpenProducts() + "}");
  }

  // ----------------------------------------------------------------------------
}
