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

import java.util.Collection;

import com.knapp.codingcontest.operations.CostFactors;
import com.knapp.codingcontest.operations.WorkStation;

public interface InputData {
  // ----------------------------------------------------------------------------

  /**
   * @return the cost-factors used for ranking
   */
  CostFactors getCostFactors();

  /**
   * @return a list of all orders
   */
  Collection<Order> getAllOrders();

  /**
   * @return a list of all work-stations (here, only 1 is used)
   */
  Collection<WorkStation> getAllWorkStations();

  // ----------------------------------------------------------------------------
}
