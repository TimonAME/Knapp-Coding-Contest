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

package com.knapp.codingcontest.operations;

import java.io.Serializable;

public interface InfoSnapshot extends Serializable {
  public enum OperationType {
    StartOrder, AssignProduct, RemoveProduct, PickOrder;
  }

  // ----------------------------------------------------------------------------

  /**
   * @return number of unfinished products
   */
  int getUnfinishedProductCount();

  /**
   * @param type
   *
   * @return the number of operations for given type
   */
  int getOperationCount(OperationType type);

  // ----------------------------------------------------------------------------

  /**
   * @return costs of current unfinished orders
   */
  double getUnfinishedOrdersCost();

  /**
   * @return costs of current product assignments
   */
  double getProductAssignmentCost();

  /**
   * The total result used for ranking.
   *
   *   (locally excludes time-based ranking factor)
   *
   * @return
   */
  double getTotalCost();

  // ----------------------------------------------------------------------------

  /**
   * @return the number of missed orders/opportunities when removing a product
   */
  long getMissedOrders();

  /**
   * @return the number of missed products/opportunities when removing a product
   */
  long getMissedProducts();

  // ----------------------------------------------------------------------------
}
