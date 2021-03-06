package betterquesting.utils;

import java.util.ArrayList;
import java.util.Map.Entry;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.logging.log4j.Level;
import betterquesting.core.BetterQuesting;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class NBTConverter
{
	/**
	 * Convert NBT tags to a JSON object
	 * @param parent
	 * @return
	 */
	private static JsonElement NBTtoJSON_Base(NBTBase tag)
	{
		if(tag == null)
		{
			return new JsonObject();
		}
		
		if(tag.getId() >= 1 && tag.getId() <= 6)
		{
			return new JsonPrimitive(getNumber(tag));
		} else if(tag instanceof NBTTagString)
		{
			NBTTagString sTag = (NBTTagString)tag;
			
			if(sTag.getString().startsWith("raw_json:")) // Prefix format = "raw_json:#:" where # is the JsonElement ID
			{
				try
				{
					String jText = sTag.getString().substring(11);
					int rawID = Integer.parseInt(sTag.getString().substring(9, 10));
					Class<? extends JsonElement> rawClass = getJsonFallback(rawID);
					JsonElement json = new Gson().fromJson(jText, rawClass);
					
					if(json != null)
					{
						return json;
					}
				} catch(Exception e)
				{
					BetterQuesting.logger.log(Level.ERROR, "Unable to parse raw json from NBT", e);
				}
			}
			
			return new JsonPrimitive(((NBTTagString)tag).getString());
		} else if(tag instanceof NBTTagCompound)
		{
			return NBTtoJSON_Compound((NBTTagCompound)tag, new JsonObject());
		} else if(tag instanceof NBTTagList)
		{
			JsonArray jAry = new JsonArray();
			
			ArrayList<NBTBase> tagList = getTagList((NBTTagList)tag);
			
			for(int i = 0; i < tagList.size(); i++)
			{
				jAry.add(NBTtoJSON_Base(tagList.get(i)));
			}
			
			return jAry;
		} else if(tag instanceof NBTTagByteArray)
		{
			JsonArray jAry = new JsonArray();
			
			for(byte b : ((NBTTagByteArray)tag).getByteArray())
			{
				jAry.add(new JsonPrimitive(b));
			}
			
			return jAry;
		} else if(tag instanceof NBTTagIntArray)
		{
			JsonArray jAry = new JsonArray();
			
			for(int i : ((NBTTagIntArray)tag).getIntArray())
			{
				jAry.add(new JsonPrimitive(i));
			}
			
			return jAry;
		} else
		{
			return new JsonObject(); // No valid types found. We'll just return this to prevent a NPE
		}
	}
	
	public static JsonObject NBTtoJSON_Compound(NBTTagCompound parent, JsonObject jObj)
	{
		if(parent == null)
		{
			return jObj;
		}
		
		for(String key : parent.getKeySet())
		{
			NBTBase tag = parent.getTag(key);
			
			if(tag == null)
			{
				continue;
			}
			
			jObj.add(key, NBTtoJSON_Base(tag));
		}
		
		return jObj;
	}
	
	/**
	 * Convert JsonObject to a NBTTagCompound
	 * @param jObj
	 * @return
	 */
	public static NBTTagCompound JSONtoNBT_Object(JsonObject jObj, NBTTagCompound tags)
	{
		if(jObj == null)
		{
			return tags;
		}
		
		for(Entry<String,JsonElement> entry : jObj.entrySet())
		{
			try
			{
				tags.setTag(entry.getKey(), JSONtoNBT_Element(entry.getValue()));
			} catch(Exception e)
			{
				continue; // Given key is not a JSON formatted NBT value
			}
		}
		
		return tags;
	}
	
	/**
	 * Used purely for array elements without tag names. Tries to interpret the tagID from the JsonElement's contents
	 * @param jObj
	 * @param type
	 * @return
	 */
	private static NBTBase JSONtoNBT_Element(JsonElement jObj)
	{
		if(jObj == null)
		{
			return new NBTTagString();
		}
		
		byte tagID = 0;
		
		if(jObj.isJsonPrimitive())
		{
			JsonPrimitive prim = jObj.getAsJsonPrimitive();
			
			if(prim.isNumber())
			{
				if(prim.getAsString().contains(".")) // Just in case we'll choose the largest possible container supporting this number type (Long or Double)
				{
					tagID = 6;
				} else
				{
					tagID = 4;
				}
			} else
			{
				tagID = 8; // Non-number primitive. Assume string
			}
		} else if(jObj.isJsonArray())
		{
			JsonArray array = jObj.getAsJsonArray();
			
			for(JsonElement entry : array)
			{
				if(entry.isJsonPrimitive() && tagID == 0) // Note: TagLists can only support Integers, Bytes and Compounds (Strings can be stored but require special handling)
				{
					try
					{
						for(JsonElement element : array)
						{
							// Make sure all entries can be bytes
							if(element.getAsLong() != element.getAsByte()) // In case casting works but overflows
							{
								throw new ClassCastException();
							}
						}
						tagID = 7; // Can be used as byte
					} catch(Exception e1)
					{
						try
						{
							for(JsonElement element : array)
							{
								// Make sure all entries can be integers
								if(element.getAsLong() != element.getAsInt()) // In case casting works but overflows
								{
									throw new ClassCastException();
								}
							}
							tagID = 11;
						} catch(Exception e2)
						{
							tagID = 9; // Is primitive however requires TagList interpretation
						}
					}
				} else if(!entry.isJsonPrimitive())
				{
					tagID = 9; // Non primitive, NBT compound list
					break;
				}
			}
		} else
		{
			tagID = 10;
		}
		
		try
		{
			if(tagID >= 1 && tagID <= 6)
			{
				return instanceNumber(jObj.getAsNumber(), tagID);
			} else if(tagID == 8)
			{
				return new NBTTagString(jObj.getAsString());
			} else if(tagID == 10)
			{
				return JSONtoNBT_Object(jObj.getAsJsonObject(), new NBTTagCompound());
			} else if(tagID == 7) // Byte array
			{
				JsonArray jAry = jObj.getAsJsonArray();
				
				byte[] bAry = new byte[jAry.size()];
				
				for(int i = 0; i < jAry.size(); i++)
				{
					bAry[i] = jAry.get(i).getAsByte();
				}
				
				return new NBTTagByteArray(bAry);
			} else if(tagID == 11)
			{
				JsonArray jAry = jObj.getAsJsonArray();
				
				int[] iAry = new int[jAry.size()];
				
				for(int i = 0; i < jAry.size(); i++)
				{
					iAry[i] = jAry.get(i).getAsInt();
				}
				
				return new NBTTagIntArray(iAry);
			} else if(tagID == 9)
			{
				JsonArray jAry = jObj.getAsJsonArray();
				NBTTagList tList = new NBTTagList();
				
				for(int i = 0; i < jAry.size(); i++)
				{
					JsonElement jElm = jAry.get(i);
					tList.appendTag(JSONtoNBT_Element(jElm));
				}
				
				return tList;
			} else if(tagID == -1) // Emergency fall back for unknown/unsupported types
			{
				NBTTagString tag = new NBTTagString("raw_json:" + getFallbackID(jObj) + ":" + new Gson().toJson(jObj));
				return tag;
			}
		} catch(Exception e)
		{
			BetterQuesting.logger.log(Level.ERROR, "An error occured while parsing JsonElement to NBTBase (" + tagID + "):", e);
		}
		
		return new NBTTagString();
	}
	
	/**
	 * Pulls the raw list out of the NBTTagList
	 * @param tag
	 * @return
	 */
	public static ArrayList<NBTBase> getTagList(NBTTagList tag)
	{
		return ObfuscationReflectionHelper.getPrivateValue(NBTTagList.class, tag, new String[]{"tagList", "field_74747_a"});
	}
	
	public static Number getNumber(NBTBase tag)
	{
		if(tag instanceof NBTTagByte)
		{
			return ((NBTTagByte)tag).getByte();
		} else if(tag instanceof NBTTagShort)
		{
			return ((NBTTagShort)tag).getShort();
		} else if(tag instanceof NBTTagInt)
		{
			return ((NBTTagInt)tag).getInt();
		} else if(tag instanceof NBTTagFloat)
		{
			return ((NBTTagFloat)tag).getFloat();
		} else if(tag instanceof NBTTagDouble)
		{
			return ((NBTTagDouble)tag).getDouble();
		} else if(tag instanceof NBTTagLong)
		{
			return ((NBTTagLong)tag).getLong();
		} else
		{
			return 0;
		}
	}
	
	public static NBTBase instanceNumber(Number num, byte type)
	{
		switch (type)
        {
            case 1:
                return new NBTTagByte(num.byteValue());
            case 2:
                return new NBTTagShort(num.shortValue());
            case 3:
                return new NBTTagInt(num.shortValue());
            case 4:
                return new NBTTagLong(num.longValue());
            case 5:
                return new NBTTagFloat(num.floatValue());
            case 6:
                return new NBTTagDouble(num.doubleValue());
            default:
            	return new NBTTagByte(num.byteValue());
        }
	}
	
	private static Class<? extends JsonElement> getJsonFallback(int id)
	{
		switch(id)
		{
			case 1:
				return JsonObject.class;
			case 2:
				return JsonArray.class;
			case 3:
				return JsonNull.class; // Do not use unless absolutely necessary!
			default:
				return JsonPrimitive.class;
		}
	}
	
	private static int getFallbackID(JsonElement json)
	{
		if(json == null)
		{
			return 3;
		}
		
		return getFallbackID(json.getClass());
	}
	
	private static int getFallbackID(Class<? extends JsonElement> json)
	{
		if(json == JsonObject.class)
		{
			return 1;
		} else if(json == JsonArray.class)
		{
			return 2;
		} else if(json == JsonNull.class)
		{
			return 3;
		} else
		{
			return 0;
		}
	}
}
