package betterquesting.quests.rewards;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import betterquesting.client.gui.GuiQuesting;
import betterquesting.client.gui.editors.json.GuiJsonObject;
import betterquesting.client.gui.misc.GuiEmbedded;
import com.google.gson.JsonObject;

public abstract class RewardBase
{
	public abstract String getUnlocalisedName();
	
	@SideOnly(Side.CLIENT)
	public String getDisplayName()
	{
		return I18n.format(this.getUnlocalisedName());
	}
	
	public abstract boolean canClaim(EntityPlayer player, NBTTagCompound choiceData);
	
	public abstract void Claim(EntityPlayer player, NBTTagCompound choiceData);
	
	public abstract void readFromJson(JsonObject json);
	
	public abstract void writeToJson(JsonObject json);
	
	@SideOnly(Side.CLIENT)
	public NBTTagCompound GetChoiceData()
	{
		return new NBTTagCompound();
	}
	
	@SideOnly(Side.CLIENT)
	public void SetChoiceData(NBTTagCompound tags)
	{
		return;
	}
	
	@SideOnly(Side.CLIENT)
	public abstract GuiEmbedded getGui(GuiQuesting screen, int posX, int posY, int sizeX, int sizeY);
	
	/**
	 * Returns a new editor screen for this Reward type to edit the given data
	 */
	@SideOnly(Side.CLIENT)
	public GuiScreen GetEditor(GuiScreen parent, JsonObject data)
	{
		return new GuiJsonObject(parent, data);
	}
}
