package betterquesting.network.handlers;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.apache.logging.log4j.Level;
import betterquesting.core.BetterQuesting;
import betterquesting.quests.QuestDatabase;
import betterquesting.quests.QuestInstance;
import betterquesting.quests.QuestLine;
import betterquesting.utils.NBTConverter;
import com.google.gson.JsonObject;

public class PktHandlerLineEdit extends PktHandler
{
	@Override
	public void handleServer(EntityPlayerMP sender, NBTTagCompound data)
	{
		if(sender == null)
		{
			return;
		}
		
		if(!sender.worldObj.getMinecraftServer().getPlayerList().canSendCommands(sender.getGameProfile()))
		{
			BetterQuesting.logger.log(Level.WARN, "Player " + sender.getName() + " (UUID:" + sender.getUniqueID() + ") tried to edit quest lines without OP permissions!");
			sender.addChatComponentMessage(new TextComponentString(TextFormatting.RED + "You need to be OP to edit quests!"));
			return; // Player is not operator. Do nothing
		}
		
		int action = !data.hasKey("action")? -1 : data.getInteger("action");
		
		if(action < 0)
		{
			BetterQuesting.logger.log(Level.ERROR, sender.getName() + " tried to perform invalid quest edit action: " + action);
			return;
		}
		
		if(action == 0) // Add new QuestLine
		{
			QuestDatabase.questLines.add(new QuestLine());
		} else if(action == 1) // Add new QuestInstance
		{
			new QuestInstance(QuestDatabase.getUniqueID(), true);
		} else if(action == 2) // Edit quest lines
		{
			QuestDatabase.readFromJson_Lines(NBTConverter.NBTtoJSON_Compound(data.getCompoundTag("Data"), new JsonObject()));
		}
		
		QuestDatabase.UpdateClients(); // Update all clients with new quest data
	}
	
	@Override
	public void handleClient(NBTTagCompound data)
	{
	}
}
