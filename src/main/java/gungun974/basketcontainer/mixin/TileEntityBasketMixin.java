package gungun974.basketcontainer.mixin;

import net.minecraft.core.block.entity.TileEntityBasket;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.container.Container;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(value = TileEntityBasket.class, remap = false)
public abstract class TileEntityBasketMixin implements Container {
	@Shadow
	@Final
	private Map<TileEntityBasket.BasketEntry, Integer> contents;

	@Shadow
	protected abstract void updateNumUnits();

	@Shadow
	protected abstract int getItemSizeUnits(Item item);

	@Shadow
	public abstract int getMaxUnits();

	@Shadow
	private int numUnitsInside;

	@Shadow
	protected abstract int calcNumUnitsInside();

	@Override
	public int getContainerSize() {
		return getMaxUnits();
	}

	@Unique
	private int getCurrentSize() {
		int total = 0;
		for (Map.Entry<TileEntityBasket.BasketEntry, Integer> entry : this.contents.entrySet()) {
			total += entry.getValue();
		}
		return total;
	}

	@Override
	public @Nullable ItemStack getItem(int i) {
		int numInsideToBlock = 0;

		for(Map.Entry<TileEntityBasket.BasketEntry, Integer> entry : this.contents.entrySet()) {
			TileEntityBasket.BasketEntry be = entry.getKey();
			int numItems = entry.getValue();
			int unitsPerItem = this.getItemSizeUnits(be.getItem()) - 1;
			numInsideToBlock += unitsPerItem * numItems;
		}

		if (i >= getCurrentSize()) {
			if (i >= getContainerSize() - numInsideToBlock) {
				return new ItemStack(260, 0, 254);
			}

			int freeUnits = this.getMaxUnits() - this.numUnitsInside;

			if (freeUnits > 0) {
				return null;
			}

			return new ItemStack(260, 0, 254);
		}

		List<Map.Entry<TileEntityBasket.BasketEntry, Integer>> list = new ArrayList<>(this.contents.entrySet());

		list.sort(Map.Entry.comparingByValue());

		int spaceIndex = 0;

		for (Map.Entry<TileEntityBasket.BasketEntry, Integer> entry : list) {
			spaceIndex += entry.getValue();

			if (i < spaceIndex) {
				TileEntityBasket.BasketEntry basketEntry = entry.getKey();

				return new ItemStack(basketEntry.id, 1, basketEntry.metadata, basketEntry.tag);
			}
		}

		return null;
	}

	@Override
	public @Nullable ItemStack removeItem(int index, int takeAmount) {
		if (index >= this.contents.size()) {
			return null;
		}

		List<Map.Entry<TileEntityBasket.BasketEntry, Integer>> list = new ArrayList<>(this.contents.entrySet());

		list.sort(Map.Entry.comparingByValue());

		int spaceIndex = 0;

		Map.Entry<TileEntityBasket.BasketEntry, Integer> entry = null;

		for (Map.Entry<TileEntityBasket.BasketEntry, Integer> searchEntry : list) {
			spaceIndex += searchEntry.getValue();

			if (index < spaceIndex) {
				entry = searchEntry;
				break;
			}
		}

		if (entry == null) {
			return null;
		}

		TileEntityBasket.BasketEntry basketEntry = entry.getKey();

		if (1 < takeAmount) {
			return null;
		}

		ItemStack virtualSlot = new ItemStack(basketEntry.id, 1, basketEntry.metadata, basketEntry.tag);

		int currentItemsInBE = this.contents.getOrDefault(basketEntry, 0);
		currentItemsInBE -= 1;
		if (currentItemsInBE <= 0) {
			this.contents.remove(basketEntry);
		} else {
			this.contents.put(basketEntry, currentItemsInBE);
		}

		this.setChanged();

		return virtualSlot;
	}

	@Override
	public void setItem(int i, @Nullable ItemStack stack) {
		if (i >= getCurrentSize()) {
			if (stack == null) {
				return;
			}
			TileEntityBasket.BasketEntry entry = new TileEntityBasket.BasketEntry(stack.itemID, stack.getMetadata(), stack.getData());
			int sizeUnits = this.getItemSizeUnits(stack.getItem());
			int freeUnits = this.getMaxUnits() - this.numUnitsInside;
			int itemsToTake = Math.min(freeUnits / sizeUnits, stack.stackSize);

			if (itemsToTake > 0) {
				stack.stackSize -= itemsToTake;
				int currentItemsInBE = this.contents.getOrDefault(entry, 0);
				currentItemsInBE += itemsToTake;
				this.contents.put(entry, currentItemsInBE);
				this.setChanged();
			}
			return;
		}

		List<Map.Entry<TileEntityBasket.BasketEntry, Integer>> list = new ArrayList<>(this.contents.entrySet());

		list.sort(Map.Entry.comparingByValue());

		int spaceIndex = 0;

		Map.Entry<TileEntityBasket.BasketEntry, Integer> entry = null;

		for (Map.Entry<TileEntityBasket.BasketEntry, Integer> searchEntry : list) {
			spaceIndex += searchEntry.getValue();

			if (i < spaceIndex) {
				entry = searchEntry;
				break;
			}
		}

		if (entry == null) {
			return;
		}

		TileEntityBasket.BasketEntry basketEntry = entry.getKey();

		if (stack == null) {
			int currentItemsInBE = this.contents.getOrDefault(basketEntry, 0);
			currentItemsInBE -= 1;
			if (currentItemsInBE <= 0) {
				this.contents.remove(basketEntry);
			} else {
				this.contents.put(basketEntry, currentItemsInBE);
			}
			this.setChanged();
			return;
		}

		TileEntityBasket.BasketEntry newEntry = new TileEntityBasket.BasketEntry(stack.itemID, stack.getMetadata(), stack.getData());
		int sizeUnits = this.getItemSizeUnits(stack.getItem());
		int freeUnits = this.getMaxUnits() - this.numUnitsInside;
		int itemsToTake = Math.min(freeUnits / sizeUnits, stack.stackSize);

		if (itemsToTake > 0) {
			this.contents.remove(basketEntry);
			stack.stackSize -= itemsToTake;
			this.contents.put(newEntry, itemsToTake);
			this.setChanged();
		}
	}

	@Override
	public String getNameTranslationKey() {
		return "container.chest.name";
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public void setChanged() {
		this.updateNumUnits();
	}

	@Override
	public boolean stillValid(Player player) {
		return false;
	}

	@Override
	public void sortContainer() {
	}
}
