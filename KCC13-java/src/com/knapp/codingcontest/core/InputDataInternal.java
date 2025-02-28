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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.knapp.codingcontest.data.Bin;
import com.knapp.codingcontest.data.InputData;
import com.knapp.codingcontest.data.Order;
import com.knapp.codingcontest.data.Position;
import com.knapp.codingcontest.data.Shelf;
import com.knapp.codingcontest.operations.CostFactors;

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

  MyPosition pos0 = new MyPosition(Position.Side.Right, Position.Offset.Bin, 1, 1);

  private final CostFactors costFactors;

  protected final Map<String, MyBin> bins = new TreeMap<>();
  protected final Map<String, MyShelf> shelves = new TreeMap<>();
  protected final Map<String, List<Shelf>> productShelves = new HashMap<>();
  protected final Map<String, MyOrder> orders = new LinkedHashMap<>();

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

  public Collection<Bin> getAllBins() {
    return Collections.unmodifiableCollection(bins.values());
  }

  public Collection<Shelf> getAllShelves() {
    return Collections.unmodifiableCollection(shelves.values());
  }

  // ----------------------------------------------------------------------------

  public void readData(final WarehouseInternal iwarehouse) throws IOException {
    readMps();
    readShelves();
    readOrders();
    iwarehouse.prepareAfterRead();
  }

  // ----------------------------------------------------------------------------
  // ----------------------------------------------------------------------------

  private void readMps() throws IOException {
    final Reader fr = new FileReader(fullFileName("mps-bins.csv"));
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(fr);
      for (String line = reader.readLine(); line != null; line = reader.readLine()) {
        line = line.trim();
        if ("".equals(line) || line.startsWith("#")) {
          continue;
        }
        // #code,side;zone;pos;ofs;
        final String[] columns = splitCsv(line);
        final String bcode = columns[0];
        final Position.Side side = Position.Side.valueOf(columns[1]);
        final int zone = Integer.parseInt(columns[2]);
        final int lengthwise = Integer.parseInt(columns[3]);
        bins.put(bcode, new MyBin(bcode, new MyPosition(side, Position.Offset.Bin, lengthwise, zone)));
      }
    } finally {
      close(reader);
      close(fr);
    }
  }

  // ............................................................................

  private void readShelves() throws IOException {
    final Map<String, Set<Shelf>> productShelves = new HashMap<>();
    final Reader fr = new FileReader(fullFileName("shelves.csv"));
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(fr);
      for (String line = reader.readLine(); line != null; line = reader.readLine()) {
        line = line.trim();
        if ("".equals(line) || line.startsWith("#")) {
          continue;
        }
        // #code;side;zone;pos;ofs;(prod-code;)*
        final String[] columns = splitCsv(line);
        final String shelfCode = columns[0];
        final Position.Side side = Position.Side.valueOf(columns[1]);
        final int zone = Integer.parseInt(columns[2]);
        final int lengthwise = Integer.parseInt(columns[3]);
        final Set<String> products = new HashSet<>();
        for (int c = 4; c < columns.length; c++) {
          final String product = columns[c].intern();
          products.add(product);
        }
        final MyShelf shelf = new MyShelf(shelfCode, new MyPosition(side, Position.Offset.Shelf, lengthwise, zone), products);
        shelves.put(shelfCode, shelf);
        for (final String product : products) {
          productShelves.computeIfAbsent(product, p -> new HashSet<>()).add(shelf);
        }
      }
      for (final Map.Entry<String, Set<Shelf>> e : productShelves.entrySet()) {
        this.productShelves.put(e.getKey(), Collections.unmodifiableList(
            e.getValue().stream().sorted((s1, s2) -> s1.getCode().compareTo(s2.getCode())).collect(Collectors.toList())));
      }
    } finally {
      close(reader);
      close(fr);
    }
  }

  // ............................................................................

  private void readOrders() throws IOException {
    final Reader fr = new FileReader(fullFileName("order-lines.csv"));
    BufferedReader reader = null;
    try {
      final Map<String, List<String>> _orders = new LinkedHashMap<>();
      reader = new BufferedReader(fr);
      for (String line = reader.readLine(); line != null; line = reader.readLine()) {
        line = line.trim();
        if ("".equals(line) || line.startsWith("#")) {
          continue;
        }
        // #order-code;product-code;
        final String[] columns = splitCsv(line);
        final String ocode = columns[0];
        final String product = columns[1].intern();
        _orders.computeIfAbsent(ocode, c -> new ArrayList<>()).add(product);
      }
      for (final Map.Entry<String, List<String>> e : _orders.entrySet()) {
        orders.put(e.getKey(), new MyOrder(e.getKey(), e.getValue()));
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

  public static class MyPosition extends Position implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int zone;

    public MyPosition(final Side side, final Offset offset, final int lengthwise, final int zone) {
      super(side, offset, lengthwise);
      this.zone = zone;
    }
  }

  // ............................................................................

  public static class MyOrder extends Order {
    private final List<String> openProducts;

    MyOrder(final Order order) {
      this(order.getCode(), order.getAllProducts());
    }

    MyOrder(final String code, final List<String> products) {
      super(code, products);
      openProducts = new ArrayList<>(products);
    }

    @Override
    public List<String> getOpenProducts() {
      return Collections.unmodifiableList(new ArrayList<>(openProducts));
    }

    void processedProduct(final String product) {
      openProducts.remove(product);
    }

    @Override
    public String toString() {
      return super.toString() + "{openProducts=" + openProducts + "}";
    }
  }

  // ............................................................................

  public static class MyBin extends Bin {
    private final List<String> products = new ArrayList<>();

    public MyBin(final String code, final MyPosition position) {
      super(code, position);
    }

    @Override
    public MyPosition getPosition() {
      return (MyPosition) super.getPosition();
    }

    void putProduct(final String product) {
      products.add(product);
    }

    List<String> finishBin() {
      final List<String> released = new ArrayList<>(products);
      products.clear();
      return released;
    }

    @Override
    public String toString() {
      return super.toString() + "{" + products + "}";
    }
  }

  // ............................................................................

  public static class MyShelf extends Shelf {
    public MyShelf(final String code, final MyPosition position, final Set<String> products) {
      super(code, position, products);
    }

    @Override
    public MyPosition getPosition() {
      return (MyPosition) super.getPosition();
    }
  }

  // ----------------------------------------------------------------------------

  public InputStat inputStat() {
    return new InputStat(this);
  }

  public static final class InputStat {
    public final int countOrders;
    public final int countProducts;
    public final int countProductCodes;

    public final double avgProductPerOrder;

    private InputStat(final InputDataInternal iinput) {
      countOrders = iinput.orders.size();
      countProducts = iinput.orders.values().stream().mapToInt(o -> o.getAllProducts().size()).sum();
      countProductCodes = (int) iinput.orders.values()
          .stream()
          .flatMap(o -> o.getAllProducts().stream())
          .sorted()
          .distinct()
          .count();
      avgProductPerOrder = (double) countProducts / countOrders;
    }
  }

  // ----------------------------------------------------------------------------
}
