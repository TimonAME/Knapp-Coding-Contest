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

public final class Product implements Comparable<Product> {
  private final String code;

  // ----------------------------------------------------------------------------

  public Product(final String code) {
    this.code = code;
  }

  // ----------------------------------------------------------------------------

  public String getCode() {
    return code;
  }

  // ----------------------------------------------------------------------------

  @Override
  public String toString() {
    return code;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + ((code == null) ? 0 : code.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Product other = (Product) obj;
    return code.equals(other.code);
  }

  @Override
  public int compareTo(final Product other) {
    return code.compareTo(other.code);
  }

  // ----------------------------------------------------------------------------
  // ----------------------------------------------------------------------------
}
