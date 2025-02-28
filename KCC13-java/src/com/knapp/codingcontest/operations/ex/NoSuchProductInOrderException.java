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

import java.util.TreeSet;

import com.knapp.codingcontest.core.InputDataInternal.MyOrder;
import com.knapp.codingcontest.core.WarehouseInternal.Operation.PutProduct;

public class NoSuchProductInOrderException extends AbstractWarehouseException {
  private static final long serialVersionUID = 1L;

  // ----------------------------------------------------------------------------

  public NoSuchProductInOrderException(final PutProduct op, final MyOrder order, final String currentProduct) {
    super(op.toResultString() + "{cp=" + currentProduct + ",op=" + new TreeSet<>(order.getAllProducts()) + "}");
  }

  // ----------------------------------------------------------------------------
}
