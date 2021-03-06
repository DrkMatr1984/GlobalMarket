package com.survivorserver.GlobalMarket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.survivorserver.GlobalMarket.Interface.Handler;
import com.survivorserver.GlobalMarket.Interface.MarketInterface;
import com.survivorserver.GlobalMarket.Interface.MarketItem;

public class InterfaceHandler {

	Market market;
	MarketStorage storage;
	List<InterfaceViewer> viewers;
	List<MarketInterface> interfaces;
	List<Handler> handlers;
	
	public InterfaceHandler(Market market, MarketStorage storage) {
		this.market = market;
		this.storage = storage;
		viewers = new ArrayList<InterfaceViewer>();
		interfaces = new ArrayList<MarketInterface>();
		handlers = new ArrayList<Handler>();
	}
	
	public void registerInterface(MarketInterface gui) {
		interfaces.add(gui);
	}
	
	public void unregisterInterface(MarketInterface gui) {
		interfaces.remove(gui);
	}
	
	public MarketInterface getInterface(String name) {
		for (MarketInterface gui : interfaces) {
			if (gui.getName().equalsIgnoreCase(name)) {
				return gui;
			}
		}
		throw new IllegalArgumentException("Interface " + name + " was not found");
	}
	
	public List<MarketInterface> getInterfaces() {
		return interfaces;
	}
	
	public void registerHandler(Handler handler) {
		handlers.add(handler);
	}
	
	public void unregisterHandler(Handler handler) {
		handlers.remove(handler);
	}
	
	public List<Handler> getHandlers() {
		return handlers;
	}
	
	public InterfaceViewer addViewer(Player player, Inventory gui, MarketInterface mInterface) {
		String name = player.getName();
		for (InterfaceViewer viewer : viewers) {
			if (viewer.getViewer().equalsIgnoreCase(name)) {
				gui = null;
				return viewer;
			}
		}
		InterfaceViewer viewer = new InterfaceViewer(name, name, gui, mInterface, player.getWorld().getName());
		viewers.add(viewer);
		return viewer;
	}
	
	public void addViewer(InterfaceViewer v) {
		for (InterfaceViewer viewer : viewers) {
			if (viewer.getViewer().equalsIgnoreCase(v.getViewer())) {
				viewers.remove(v);
			}
		}
		viewers.add(v);
	}
	
	public InterfaceViewer findViewer(String player) {
		for (InterfaceViewer viewer : viewers) {
			if (viewer.getViewer().equalsIgnoreCase(player)) {
				return viewer;
			}
		}
		return null;
	}
	
	public void removeViewer(InterfaceViewer viewer) {
		viewers.remove(viewer);
	}
	
	public void openGui(InterfaceViewer viewer) {
		market.getServer().getPlayer(viewer.getViewer()).openInventory(viewer.getGui());
	}
	
	public void openInterface(Player player, String search, String marketInterface) {
		MarketInterface mInterface = getInterface(marketInterface);
		InterfaceViewer viewer = addViewer(player,
				market.getServer().createInventory(player, mInterface.getSize(), market.enableMultiworld() ?  mInterface.getTitle() + " (" + player.getWorld().getName() + ")" :  mInterface.getTitle()),
				mInterface);
		viewer.setSearch(search);
		refreshInterface(viewer);
		openGui(viewer);
	}
	
	public void refreshSlot(InterfaceViewer viewer, int slot, MarketItem item) {
		MarketInterface mInterface = viewer.getInterface();
		Inventory inv = viewer.getGui();
		boolean left = false;
		boolean shift = false;
		if (viewer.getLastAction() != null) {
			if (viewer.getLastAction() == InventoryAction.PICKUP_ALL) {
				if (viewer.getLastActionSlot() == slot) {
					if (viewer.getLastItem() >= 0 && viewer.getLastItem() == item.getId()) {
						left = true;
					}
				}
			}
		}
		if (viewer.getLastAction() != null) {
			if (viewer.getLastAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
				if (viewer.getLastActionSlot() == slot) {
					if (viewer.getLastItem() >= 0 && viewer.getLastItem() == item.getId()) {
						shift = true;
					}
				}
			}
		}
		inv.setItem(slot, mInterface.prepareItem(item, viewer, viewer.getPage(), slot, left, shift));
	}
	
	public void refreshInterface(InterfaceViewer viewer) {
		MarketInterface mInterface = viewer.getInterface();
		Map<Integer, Integer> boundSlots = new HashMap<Integer, Integer>();
		List<MarketItem> contents = mInterface.getContents(viewer);
		Inventory inv = viewer.getGui();
		ItemStack[] invContents = new ItemStack[viewer.getGui().getSize()];
		mInterface.onInterfacePrepare(viewer, contents, invContents, inv);
		String search = viewer.getSearch();
		if (search != null) {
			contents = mInterface.doSearch(viewer, viewer.getSearch());
		}
		int slot = 0;
		int p = 0;
		int n = viewer.getPage() * (invContents.length - 9);
		boolean clicked = false;
		Iterator<MarketItem> iterator = contents.iterator();
		while (iterator.hasNext()) {
			MarketItem marketItem = iterator.next();
			p++;
			if (slot < (invContents.length - 9)) {
				boolean left = false;
				boolean shift = false;
				if (viewer.getLastAction() != null) {
					if (viewer.getLastAction() == InventoryAction.PICKUP_ALL) {
						if (viewer.getLastActionSlot() == slot) {
							if (viewer.getLastItem() >= 0 && viewer.getLastItem() == marketItem.getId()) {
								clicked = true;
								left = true;
							}
						}
					}
				}
				if (viewer.getLastAction() != null) {
					if (viewer.getLastAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
						if (viewer.getLastActionSlot() == slot) {
							if (viewer.getLastItem() >= 0 && viewer.getLastItem() == marketItem.getId()) {
								clicked = true;
								shift = true;
							}
						}
					}
				}
				ItemStack item = mInterface.prepareItem(marketItem, viewer, p, slot, left, shift);
				boundSlots.put(slot, marketItem.getId());
				invContents[slot] = item;
			}
			slot++;
		}
		boolean nextPage = false;
		boolean prevPage = false;
		int t = mInterface.getTotalNumberOfItems(viewer);
		if (n < t) {
			nextPage = true;
		}
		if (n > (invContents.length - 9)) {
			prevPage = true;
		}
		mInterface.buildFunctionBar(market, this, viewer, invContents, prevPage, nextPage);
		inv.setContents(invContents);
		viewer.setBoundSlots(boundSlots);
		if (!clicked) {
			viewer.resetActions();
		}
	}
	
	public void refreshViewer(InterfaceViewer viewer, String view) {
		if (viewer.getInterface().getName().equalsIgnoreCase(view)) {
			refreshInterface(viewer);
		}
	}
	
	public void refreshViewer(String name, String view) {
		InterfaceViewer viewer = findViewer(name);
		if (viewer != null) {
			refreshViewer(viewer, view);
		}
		for (Handler handler : handlers) {
			handler.updateViewer(name);
		}
	}
	
	public void updateAllViewers() {
		List<InterfaceViewer> inactive = new ArrayList<InterfaceViewer>();
		for (InterfaceViewer viewer : viewers) {
			Player player = market.getServer().getPlayer(viewer.getViewer());
			if (player == null) {
				inactive.add(viewer);
				continue;
			} else if (player.getOpenInventory() == null) {
				inactive.add(viewer);
				continue;
			}
			refreshViewer(viewer, viewer.getInterface().getName());
		}
		if (!inactive.isEmpty()) {
			for (InterfaceViewer viewer : inactive) {
				removeViewer(viewer);
			}
		}
		for (Handler handler : handlers) {
			handler.updateAllViewers();
		}
	}
	
	public void closeAllInterfaces() {
		for (InterfaceViewer viewer : viewers) {
			Player player = market.getServer().getPlayer(viewer.getViewer());
			if (player != null) {
				player.closeInventory();
				player.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + market.getLocale().get("interface_closed_due_to_reload"));
			}
		}
	}
	
	public List<InterfaceViewer> getAllViewers() {
		return viewers;
	}
	
	public boolean isAdmin(String name) {
		if (market.getPerms() == null) {
			Player player = market.getServer().getPlayer(name);
			if (player != null) {
				return player.hasPermission("globalmarket.admin");
			} else {
				return false;
			}
		}
		return market.getPerms().playerHas(market.getServer().getWorlds().get(0).getName(), name, "globalmarket.admin");
	}
}
