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

package com.knapp.codingcontest.core;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.knapp.codingcontest.data.InputData;
import com.knapp.codingcontest.data.Order;
import com.knapp.codingcontest.data.Product;
import com.knapp.codingcontest.operations.CostFactors;
import com.knapp.codingcontest.operations.WorkStation;

public class InputDataInternal implements InputData {
  // ----------------------------------------------------------------------------

  private static final String PATH_INPUT_DATA;
  static {
    try {
      PATH_INPUT_DATA = new File("./data").getCanonicalPath();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  // ----------------------------------------------------------------------------

  private final String dataPath;

  private final CostFactors costFactors;

  protected final Map<String, WorkStationInternal> workStations = new LinkedHashMap<>();
  protected final Map<String, MyOrder> orders = new LinkedHashMap<>();
  protected final Set<Product> validProducts = new TreeSet<>();

  // ----------------------------------------------------------------------------

  public InputDataInternal(final CostFactors costFactors) {
    this(InputDataInternal.PATH_INPUT_DATA, costFactors);
  }

  public InputDataInternal(final String dataPath, final CostFactors costFactors) {
    this.dataPath = dataPath;
    this.costFactors = costFactors;
  }

  @Override
  public String toString() {
    return "InputData@" + dataPath;
  }

  // ----------------------------------------------------------------------------

  @Override
  public CostFactors getCostFactors() {
    return costFactors;
  }

  @Override
  public Collection<Order> getAllOrders() {
    return Collections.unmodifiableCollection(orders.values());
  }

  @Override
  public Collection<WorkStation> getAllWorkStations() {
    return Collections.unmodifiableCollection(workStations.values());
  }

  // ----------------------------------------------------------------------------

  public boolean isValidProduct(final Product product) {
    return validProducts.contains(product);
  }

  // ----------------------------------------------------------------------------

  public void readData(final WarehouseInternal iwarehouse) throws IOException {
    readWorkStations(iwarehouse);
    readOrders();
  }

  // ----------------------------------------------------------------------------
  // ----------------------------------------------------------------------------

  private void readOrders() throws IOException {
    final Reader fr = new FileReader(fullFileName("order-lines.csv"));
    BufferedReader reader = null;
    try {
      final Map<String, List<Product>> _orders = new LinkedHashMap<>();
      reader = new BufferedReader(fr);
      for (String line = reader.readLine(); line != null; line = reader.readLine()) {
        line = line.trim();
        if ("".equals(line) || line.startsWith("#")) {
          continue;
        }
        // order-code;product-code;
        final String[] columns = splitCsv(line);
        final String ocode = columns[0];
        final String pcode = columns[1];
        validProducts.add(new Product(pcode));
        _orders.computeIfAbsent(ocode, c -> new ArrayList<Product>()).add(new Product(pcode));
      }
      for (final Map.Entry<String, List<Product>> e : _orders.entrySet()) {
        orders.put(e.getKey(), new MyOrder(e.getKey(), e.getValue()));
      }
    } finally {
      close(reader);
      close(fr);
    }
  }

  // ............................................................................

  private void readWorkStations(final WarehouseInternal iwarehouse) throws IOException {
    final Reader fr = new FileReader(fullFileName("work-stations.csv"));
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(fr);
      for (String line = reader.readLine(); line != null; line = reader.readLine()) {
        line = line.trim();
        if ("".equals(line) || line.startsWith("#")) {
          continue;
        }
        // #code;#product-slots;#order-slots;(*|(allowed-product-code;)*)
        final String[] columns = splitCsv(line);
        final String wscode = columns[0];
        final int productSlots = Integer.parseInt(columns[1]);
        final int orderSlots = Integer.parseInt(columns[2]);
        for (int i = 3; i < columns.length; i++) {
          final String pcode = columns[i];
          final Product product = new Product(pcode);
          validProducts.add(product);
        }
        workStations.put(wscode, new WorkStationInternal(iwarehouse, wscode, productSlots, orderSlots));
      }
    } finally {
      close(reader);
      close(fr);
    }
  }

  // ----------------------------------------------------------------------------

  protected File fullFileName(final String fileName) {
    final String fullFileName = dataPath + File.separator + fileName;
    return new File(fullFileName);
  }

  protected void close(final Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (final IOException exception) {
        exception.printStackTrace(System.err);
      }
    }
  }

  // ----------------------------------------------------------------------------

  protected String[] splitCsv(final String line) {
    return line.split(";");
  }

  // ----------------------------------------------------------------------------
  // ----------------------------------------------------------------------------

  public static class MyOrder extends Order {
    private final List<Product> openProducts;

    MyOrder(final Order order) {
      this(order.getCode(), order.getAllProducts());
    }

    MyOrder(final String code, final List<Product> products) {
      super(code, products);
      openProducts = new ArrayList<>(products);
    }

    @Override
    public List<Product> getOpenProducts() {
      return Collections.unmodifiableList(new ArrayList<>(openProducts));
    }

    void pickedProduct(final Product product) {
      openProducts.remove(product);
    }
  }

  // ----------------------------------------------------------------------------

  public InputStat inputStat() {
    return new InputStat(this);
  }

  public static final class InputStat {
    public final int countWorkStations;
    public final int countOrders;
    public final int countProducts;
    public final int countProductCodes;

    public final double avgProductPerOrder;

    private InputStat(final InputDataInternal iinput) {
      countWorkStations = iinput.workStations.size();
      countOrders = iinput.orders.size();
      countProducts = iinput.orders.values().stream().mapToInt(o -> o.getAllProducts().size()).sum();
      countProductCodes = (int) iinput.orders.values()
          .stream()
          .flatMap(o -> o.getAllProducts().stream())
          .map(p -> p.getCode())
          .sorted()
          .distinct()
          .count();
      avgProductPerOrder = (double) countProducts / countOrders;
    }
  }

  // ----------------------------------------------------------------------------
}
