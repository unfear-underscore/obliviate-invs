package mc.obliviate.inventory;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import mc.obliviate.inventory.advancedslot.AdvancedSlot;
import mc.obliviate.inventory.advancedslot.AdvancedSlotManager;
import mc.obliviate.inventory.pagination.PaginationManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class Gui implements InventoryHolder {

	private final Map<Integer, Icon> registeredIcons = new HashMap<>();
	private final String id;
	private AdvancedSlotManager advancedSlotManager = null;
	private final InventoryType inventoryType;
	public Player player;
	private PaginationManager paginationManager = null;
	private Inventory inventory;
	private String title;
	private int size;
	private boolean isClosed = false;

	public Gui(Player player, String id, String title, int rows) {
		this.player = player;
		this.size = rows * 9;
		this.title = title;
		this.id = id;
		this.inventoryType = InventoryType.CHEST;
	}

	public Gui(Player player, String id, String title, InventoryType inventoryType) {
		this.player = player;
		this.size = Integer.MAX_VALUE;
		this.title = title;
		this.id = id;
		this.inventoryType = inventoryType;
	}


	/**
	 * @param e event
	 * @return force to uncancel
	 */
	public boolean onClick(InventoryClickEvent e) {
		return false;
	}

	/**
	 * @param e event
	 * @return force to uncancel
	 */
	public boolean onDrag(InventoryDragEvent e) {
		return false;
	}


	public void onOpen(InventoryOpenEvent event) {

	}

	public void onClose(InventoryCloseEvent event) {

	}

	public void open() {
		final Gui gui = InventoryAPI.getInstance().getPlayersCurrentGui(player);
		if (gui != null) {
			//call Bukkit's inventory close event
			Bukkit.getPluginManager().callEvent(new InventoryCloseEvent(player.getOpenInventory()));
		}

		InventoryAPI.getInstance().getPlayers().put(player.getUniqueId(), this);

		if (inventoryType.equals(InventoryType.CHEST)) {
			inventory = Bukkit.createInventory(null, size, title);
		} else {
			inventory = Bukkit.createInventory(null, inventoryType, title);
		}

		player.openInventory(inventory);
	}

	public void fillGui(Icon icon) {
		for (int slot = 0; slot < size; slot++) {
			addItem(slot, icon);
		}
	}

	public void fillGui(ItemStack item) {
		fillGui(new Icon(item));
	}

	public void fillGui(Material material) {
		fillGui(new Icon(material));
	}

	public void fillGui(Icon icon, Integer... blacklisted_slots) {
		for (int slot = 0; slot < size; slot++) {
			if (!checkContainsInt(slot, blacklisted_slots)) {
				addItem(slot, icon);
			}
		}
	}

	public void fillRow(Icon item, int row) {
		for (int i = 0; i < 9; i++) {
			addItem((row * 9 + i), item);
		}
	}

	public void fillColumn(Icon item, int column) {
		for (int i = 0; i < 9; i++) {
			addItem((i * 9 + column), item);
		}
	}

	private boolean checkContainsInt(int i, Integer... ints) {
		for (int j : ints) {
			if (j == i) {
				return true;
			}
		}
		return false;
	}

	public void addItem(int slot, Icon item) {
		if (inventory.getSize() <= slot) {
			throw new IndexOutOfBoundsException("Slot cannot be bigger than inventory size! [ " + slot + " >= " + inventory.getSize() + " ]");
		}
		if (item == null) {
			throw new NullPointerException("Item cannot be null!");
		}

		registeredIcons.remove(slot);
		registeredIcons.put(slot, item);
		inventory.setItem(slot, item.getItem());
	}

	public void addItem(Icon item, Integer... slots) {
		for (int slot : slots) {
			addItem(slot, item);
		}
	}

	public void addItem(int slot, ItemStack item) {
		addItem(slot, new Icon(item));
	}

	public void addItem(ItemStack item) {
		addItem(inventory.firstEmpty(), new Icon(item));
	}

	public void addItem(Material material) {
		addItem(inventory.firstEmpty(), new Icon(material));
	}

	public void addItem(int slot, Material material) {
		addItem(slot, new Icon(material));
	}

	public void updateTask(int runLater, int period, final Consumer<BukkitTask> update) {
		final BukkitTask[] bukkitTask = new BukkitTask[]{null};

		if (InventoryAPI.getInstance() != null) {
			bukkitTask[0] = (new BukkitRunnable() {
				public void run() {
					if (!isClosed()) {
						update.accept(bukkitTask[0]);
					} else {
						cancel();
					}

				}


			}).runTaskTimer(getPlugin(), runLater, period);
		}

	}


	@NotNull
	@Contract("_,_ -> new")
	public AdvancedSlot addAdvancedIcon(int slot, Icon item) {
		final AdvancedSlot aSlot = new AdvancedSlot(slot, item, getAdvancedSlotManager());
		getAdvancedSlotManager().registerSlot(aSlot);
		aSlot.resetSlot();
		return aSlot;
	}


	@NotNull
	public Map<Integer, Icon> getItems() {
		return registeredIcons;
	}

	@NotNull
	public String getId() {
		return id;
	}

	@NotNull
	public AdvancedSlotManager getAdvancedSlotManager() {
		if (advancedSlotManager == null) advancedSlotManager = new AdvancedSlotManager(this);
		return advancedSlotManager;
	}

	@NotNull
	public PaginationManager getPaginationManager() {
		if (paginationManager == null) {
			paginationManager = new PaginationManager(this);
		}
		return paginationManager;
	}

	@Override
	@NotNull
	public Inventory getInventory() {
		return inventory;
	}

	@NotNull
	public String getTitle() {
		return title;
	}

	/**
	 * Sets title of GUI. Without update.
	 *
	 * @param title new title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Automatically updates GUI title and reopens inventory
	 *
	 * @param titleUpdate new title
	 */
	public void sendTitleUpdate(String titleUpdate) {
		this.title = titleUpdate;
		open();
	}

	/**
	 * Automatically updates GUI size and reopens inventory
	 *
	 * @param sizeUpdate new size
	 */
	public void sendSizeUpdate(int sizeUpdate) {
		this.size = sizeUpdate;
		open();
	}

	@NotNull
	public int getSize() {
		return size;
	}

	@NotNull
	public void setSize(int size) {
		this.size = size;
	}

	@NotNull
	public Plugin getPlugin() {
		return InventoryAPI.getInstance().getPlugin();
	}

	@NotNull
	public boolean isClosed() {
		return isClosed;
	}

	public void setClosed(boolean closed) {
		this.isClosed = closed;
	}

}