import java.util.Date;
import java.util.ArrayList;
import java.util.List;

interface Shippable {
    String getName();
    double getWeight();
}

abstract class Item {
    protected String name;
    protected double price;
    protected int stock;

    public Item(String name, double price, int stock) {
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }

    public void reduceStock(int amount) {
        if (amount > stock) throw new RuntimeException("Not enough stock");
        stock -= amount;
    }

    public abstract boolean canBuy(int amount);
}

class RegularItem extends Item {
    public RegularItem(String name, double price, int stock) {
        super(name, price, stock);
    }

    @Override
    public boolean canBuy(int amount) {
        return amount <= stock;
    }
}

class PerishableItem extends Item {
    private Date expiryDate;

    public PerishableItem(String name, double price, int stock, Date expiryDate) {
        super(name, price, stock);
        this.expiryDate = expiryDate;
    }

    @Override
    public boolean canBuy(int amount) {
        return amount <= stock && expiryDate.after(new Date());
    }
}

class ShippableItem extends Item implements Shippable {
    private double weight;

    public ShippableItem(String name, double price, int stock, double weight) {
        super(name, price, stock);
        this.weight = weight;
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public boolean canBuy(int amount) {
        return amount <= stock;
    }
}

class CartItem {
    private Item item;
    private int quantity;

    public CartItem(Item item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    public Item getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getTotalPrice() {
        return item.getPrice() * quantity;
    }
}

class Cart {
    private List<CartItem> items = new ArrayList<>();

    public void add(Item item, int quantity) {
        if (!item.canBuy(quantity)) {
            throw new RuntimeException("Item not available");
        }
        items.add(new CartItem(item, quantity));
    }

    public List<CartItem> getItems() {
        return new ArrayList<>(items);
    }

    public double getSubtotal() {
        return items.stream().mapToDouble(CartItem::getTotalPrice).sum();
    }

    public List<Shippable> getShippables() {
        List<Shippable> result = new ArrayList<>();
        for (CartItem cartItem : items) {
            if (cartItem.getItem() instanceof Shippable) {
                result.add((Shippable) cartItem.getItem());
            }
        }
        return result;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}

class User {
    private double balance;

    public User(double balance) {
        this.balance = balance;
    }

    public double getBalance() {
        return balance;
    }

    public void deduct(double amount) {
        if (amount > balance) throw new RuntimeException("Not enough money");
        balance -= amount;
    }
}

class Shipping {
    public void process(List<Shippable> items) {
        if (items.isEmpty()) return;

        System.out.println("** Shipping **");
        double total = 0;
        for (Shippable item : items) {
            System.out.printf("%s - %.1fkg\n", item.getName(), item.getWeight());
            total += item.getWeight();
        }
        System.out.printf("Total: %.1fkg\n\n", total);
    }
}

class Shop {
    private Shipping shipping = new Shipping();

    public void checkout(User user, Cart cart) {
        if (cart.isEmpty()) throw new RuntimeException("Empty cart");

        for (CartItem cartItem : cart.getItems()) {
            if (!cartItem.getItem().canBuy(cartItem.getQuantity())) {
                throw new RuntimeException(cartItem.getItem().getName() + " unavailable");
            }
        }

        double subtotal = cart.getSubtotal();
        double shippingFee = cart.getShippables().size() * 10;
        double total = subtotal + shippingFee;

        if (total > user.getBalance()) throw new RuntimeException("Not enough money");

        shipping.process(cart.getShippables());

        for (CartItem cartItem : cart.getItems()) {
            cartItem.getItem().reduceStock(cartItem.getQuantity());
        }

        user.deduct(total);

        System.out.println("** Receipt **");
        cart.getItems().forEach(item -> 
            System.out.printf("%d x %s = %.2f\n", 
                item.getQuantity(), 
                item.getItem().getName(), 
                item.getTotalPrice()));
        System.out.printf("Subtotal: %.2f\n", subtotal);
        System.out.printf("Shipping: %.2f\n", shippingFee);
        System.out.printf("Total: %.2f\n", total);
        System.out.printf("Remaining: %.2f\n", user.getBalance());
    }
}

public class Main {
    public static void main(String[] args) {
        Item cheese = new PerishableItem("Cheese", 100, 10, 
            new Date(System.currentTimeMillis() + 86400000 * 7));
        Item biscuits = new ShippableItem("Biscuits", 150, 5, 0.7);
        Item scratchCard = new RegularItem("Scratch Card", 50, 100);

        User customer = new User(2000);
        Cart cart = new Cart();

        cart.add(cheese, 2);
        cart.add(biscuits, 1);
        cart.add(scratchCard, 1);

        new Shop().checkout(customer, cart);
    }
}