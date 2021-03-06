package freetrading.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import freetrading.trading_system.ISellable;
import freetrading.trading_system.TradeOffer;
import freetrading.trading_system.TradingSystem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityVillager.ITradeList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerCareer;

public class InventoryFreeTradingMerchant extends Container implements IInventory {

	public final EntityVillager merchant;
	private final MerchantRecipeList recipeList = new MerchantRecipeList();
	public final List<TradeOffer> tradeOffers = new ArrayList<TradeOffer>();
	public final List<TradeOffer> playerTradeOffers = new ArrayList<TradeOffer>();

	public InventoryFreeTradingMerchant(EntityVillager theMerchantIn) {
		merchant = theMerchantIn;
		this.onCraftMatrixChanged(this);
	}

	@Override
	public String getName() {
		VillagerCareer career = merchant.getProfessionForge().getCareer(merchant.careerId);
		return career.getName();
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}
	
	@Override
	public Slot getSlot(int slotId) {
		return null;
	}

	@Override
	public void onCraftMatrixChanged(IInventory inventoryIn) {
		tradeOffers.clear();
		VillagerCareer career = merchant.getProfessionForge().getCareer(merchant.careerId);
		Map<String, Set<ISellable>> gbp = TradingSystem.instance.goodsByMerchantAndCareer.get(merchant.getProfessionForge().getRegistryName());
		if(gbp==null)
			return;
		Set<ISellable> sellables = gbp.get(career.getName());
		if(sellables==null)
			return;
		for(ISellable sellable:sellables) {
			tradeOffers.add(sellable.getTradeOffer(merchant));
		}
	}
	
	@Override
	public ITextComponent getDisplayName() {
		return new TextComponentString(getName());
	}

	@Override
	public int getSizeInventory() {
		return recipeList.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : this.inventoryItemStacks) {
			if (!itemstack.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getStackInSlot(int index) {
		if(index>=this.inventoryItemStacks.size())
			return ItemStack.EMPTY;
		return this.inventoryItemStacks.get(index);
	}

	@Override
	public ItemStack decrStackSize(int index, int count) {
		return ItemStackHelper.getAndSplit(this.inventoryItemStacks, index, count);
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		ItemStack stack = ItemStackHelper.getAndRemove(this.inventoryItemStacks, index);
		return stack;
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack stack) {
		this.inventoryItemStacks.set(index, stack);
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public void markDirty() {
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer player) {
		return this.merchant.getCustomer() == player;
	}

	@Override
	public void openInventory(EntityPlayer player) {
	}

	@Override
	public void closeInventory(EntityPlayer player) {
	}

	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack) {
		return true;
	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {
	}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clear() {
		inventoryItemStacks.clear();
	}

	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {
		return playerIn.getDistanceSq(merchant)<16d;
	}
}
