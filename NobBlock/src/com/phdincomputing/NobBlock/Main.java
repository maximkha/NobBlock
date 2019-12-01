package com.phdincomputing.NobBlock;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.World;
import org.bukkit.World.Environment;

public class Main extends JavaPlugin implements Listener {
	
	private int scan = 6;
	
	@Override
	public void onEnable() {
		getLogger().info("Valve Nob Block Loaded");
		//PlayerAffected.add("MintyDev");
		PlayerAffected = getConfig().getStringList("PlayersAffected.cnfg");
		deserializeDiamondBlockLoc(getConfig().getStringList("DiamondBlockLoc.cnfg"));
		deserializeChunks(getConfig().getStringList("Chunks.cnfg"));
		
		if (PlayerAffected == null) { getLogger().warning("PlayerAffected == null"); PlayerAffected = new ArrayList<String>(); }
		else if (PlayerAffected.size() > 0) getLogger().info("PlayerAffected[0]: " + PlayerAffected.get(0));
		
		if (Chunks.size() > 0) getLogger().info("Chunks[0]: [" + Chunks.get(0)[0] + "," + Chunks.get(0)[1] + "]");
		
		if (DiamondBlockLoc.size() > 0) getLogger().info("DiamondBlockLoc[0]: [" + DiamondBlockLoc.get(0).getBlockX() + "," + DiamondBlockLoc.get(0).getBlockY() + "," + DiamondBlockLoc.get(0).getBlockZ() + "]");
		
		registerEvents();
	}
	
	@Override
	public void onDisable() {
		getLogger().info("Valve Nob Cheat Unloaded");
		getConfig().set("PlayersAffected.cnfg", PlayerAffected);
		getConfig().set("DiamondBlockLoc.cnfg", serializeDiamondBlockLoc());
		getConfig().set("Chunks.cnfg", serializeChunks());
		saveConfig();
	}
	
	public void registerEvents() {
	    PluginManager pm = Bukkit.getPluginManager();
	    pm.registerEvents(this, this);
	}
	
	private List<String> serializeChunks()
	{
		List<String> ret = new ArrayList<String>();
		for (int i = 0; i < Chunks.size(); i++)
		{
			String c = Chunks.get(i)[0] + "," + Chunks.get(i)[1] + "," + Chunks.get(i)[2] + "," + Chunks.get(i)[3];
			ret.add(c);
		}
		return ret;
	}
	
	private void deserializeChunks(List<String> data)
	{
		for (int i = 0; i < data.size(); i++)
		{
			String c = data.get(i);
			String[] strs = c.split(",");
			if (strs.length != 4) { Bukkit.getLogger().warning("Chunk Error"); continue; }
			int x = Integer.parseInt(strs[0]);
			int z = Integer.parseInt(strs[1]);
			int s = Integer.parseInt(strs[2]);
			int e = Integer.parseInt(strs[3]);
			Chunks.add(new int[] { x, z, s, e });
		}
	}
	
	private void deserializeDiamondBlockLoc(List<String> data)
	{
		for (int i = 0; i < data.size(); i++)
		{
			String c = data.get(i);
			String[] strs = c.split(",");
			if (strs.length != 3) { Bukkit.getLogger().warning("DiamondBlockLoc Error: " + c + "['" + strs[0] + "','" + strs[1] + "','" + strs[2] + "']"); continue; }
			int x = Integer.parseInt(strs[0]);
			int y = Integer.parseInt(strs[1]);
			int z = Integer.parseInt(strs[2]);
			Location loc = new Location(this.getServer().getWorld("world"), x, y, z);
			DiamondBlockLoc.add(loc);
		}
	}
	
	private List<String> serializeDiamondBlockLoc()
	{
		List<String> ret = new ArrayList<String>();
		for (int i = 0; i < DiamondBlockLoc.size(); i++)
		{
			Location c = DiamondBlockLoc.get(i);
			ret.add(c.getBlockX() + "," + c.getBlockY() + "," + c.getBlockZ());
		}
		return ret;
	}
	
	//private List<Tuple<String, Boolean>> PlayerFirstMove = new ArrayList<Tuple<String, Boolean>>();
	private List<String> PlayerAffected = new ArrayList<String>();
	private List<int[]> Chunks = new ArrayList<int[]>();
	//private List<Tuple<int[], List<Location>>> DiamondBlockLoc = new ArrayList<Tuple<int[], List<Location>>>();
	private List<Location> DiamondBlockLoc = new ArrayList<Location>();
	private List<Tuple<String, int[]>> PlayerPrevChunk = new ArrayList<Tuple<String, int[]>>();
	private boolean debug = false;
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (!sender.isOp()) return true;
		if (label.equalsIgnoreCase("nobblock"))
		{
			if (args.length > 0)
			{
				if (args[0].equalsIgnoreCase("reset"))
				{
					PlayerAffected = new ArrayList<String>();
					Chunks = new ArrayList<int[]>();
					DiamondBlockLoc = new ArrayList<Location>();
					PlayerPrevChunk = new ArrayList<Tuple<String, int[]>>();
				}
				else if (args[0].equalsIgnoreCase("resend"))
				{
					if (args.length > 1)
					{
						if (args[1].equalsIgnoreCase("all"))
						{
							sendBlockChangeToAffected();
							sender.sendMessage("Resent to all affected");
						}
						else
						{
							Player p = this.getServer().getPlayer(args[1]);
							if (p == null) { sender.sendMessage("That user does not exist"); return false; }
							if (!p.isOnline()) { sender.sendMessage("That user is not online"); return false; }
							//sendBlockChangeToPlayer(p);
							sendValidChunksToPlayer(scan, p.getName());
						}
					} else return false;
				}
				else if (args[0].equalsIgnoreCase("list"))
				{
					if (args.length > 1)
					{
						if (args[1].equalsIgnoreCase("add"))
						{
							if (args.length > 2)
							{
								Player p = this.getServer().getPlayer(args[2]);
								if (p == null) { sender.sendMessage("That user does not exist"); return false; }
								if (!p.isOnline()) { sender.sendMessage("That user is not online"); return false; }
								PlayerAffected.add(p.getName());
								//sendBlockChangeToPlayer(p);
								if (isNewChunk(p.getName()) || Chunks.contains(new int[] { p.getWorld().getChunkAt(p.getLocation()).getX(), p.getWorld().getChunkAt(p.getLocation()).getZ()}))
						    	{
						    		processChunks(scan, p.getName());
						    	}
								sendValidChunksToPlayer(scan, p.getName());
							} else return false;
						}
						else if (args[1].equalsIgnoreCase("remove"))
						{
							if (args.length > 2)
							{
								Player p = this.getServer().getPlayer(args[2]);
								if (p == null) { sender.sendMessage("That user does not exist"); return false; }
								if (!p.isOnline()) { sender.sendMessage("That user is not online"); return false; }
								boolean b = PlayerAffected.remove(p.getName());
								if (!b) { sender.sendMessage("That user is not in the list"); return false; }
								else sender.sendMessage(p.getName()+" was removed from the list");
							} else return false;
						}
						else if (args[1].equalsIgnoreCase("all"))
						{
							sender.sendMessage("'" + this.getServer().getPlayer(sender.getName()).getWorld().getName() + "', '" + this.getServer().getPlayer(sender.getName()).getWorld().getEnvironment() + "'");
							
							for (int i = 0; i < PlayerAffected.size(); i++)
							{
								sender.sendMessage("* " + PlayerAffected.get(i));
							}
						}
						
					} else return false;
				} else return false;
				
			} else return false;
		}
		return true;
	}
	
//	@EventHandler
//	public void onChunkLoad(final ChunkLoadEvent event)
//	{
//		
//	}
	
	private boolean processChunk(Chunk chunk)
	{
		//Chunk chunk = event.getChunk();
		int[] chunkid = {chunk.getX(), chunk.getZ()};
		if (!hasSeenChunk(chunkid[0], chunkid[1]))
		{
			if (debug) Bukkit.broadcastMessage("Scanning chunk [" + chunkid[0] + "," + chunkid[1] + "]");
			Bukkit.getLogger().info("Scanning chunk [" + chunkid[0] + "," + chunkid[1] + "]");
			//New chunk
			indexChunk(chunk);
			//Send the fake block updates to the player
			//sendBlockChangeToAffected();
			return true;
		}
		return false;
	}
	
	private void processChunks(int cCount, String player)
	{
		Player p = this.getServer().getPlayer(player);
		if (!p.isOnline()) return;
		if (p.getWorld().getEnvironment() != Environment.NORMAL) return;
		if (!p.getWorld().getName().contentEquals("world")) return;
		Chunk chunk = p.getWorld().getChunkAt(p.getLocation());
		int cChunkX = chunk.getX();
		int cChunkZ = chunk.getZ();
		if (cCount % 2 != 0) cCount--; //cCount++;
		int half = cCount / 2;
		//Make sure we center it around the player chunk
		for (int x = 0; x < cCount + 1; x++)
		{
			for (int z = 0; z < cCount + 1; z++)
			{
				int rChunkX = (cChunkX - half) + x;
				int rChunkZ = (cChunkZ - half) + z;
				
				if (Chunks.contains(new int[] { rChunkX, rChunkZ})) continue;
				
				Chunk rChunk = p.getPlayer().getWorld().getChunkAt(rChunkX, rChunkZ);
				processChunk(rChunk);
			}
		}
	}
	
	private void sendValidChunksToPlayer(int cCount, String player)
	{
		Player p = this.getServer().getPlayer(player);
		if (!p.isOnline()) return;
		if (p.getWorld().getEnvironment() != Environment.NORMAL) return;
		if (!p.getWorld().getName().contentEquals("world")) return;
		Chunk chunk = p.getWorld().getChunkAt(p.getLocation());
		int cChunkX = chunk.getX();
		int cChunkZ = chunk.getZ();
		if (cCount % 2 != 0) cCount--;
		int half = cCount / 2;
		final int tCCount = cCount;
		//Make sure we center it around the player chunk
		this.getServer().getScheduler().runTask(this, new Runnable() {
			public void run()
			{
				int[][] chunksToLoad = new int[(tCCount + 1)*(tCCount + 1)][];
				for (int x = 0; x < tCCount + 1; x++)
				{
					for (int z = 0; z < tCCount + 1; z++)
					{
						int rChunkX = (cChunkX - half) + x;
						int rChunkZ = (cChunkZ - half) + z;
						if (debug) Bukkit.broadcastMessage("Sending chunks to " + player);
						int arrIndex = x * (tCCount + 1) + z;
						chunksToLoad[arrIndex] = new int[] { rChunkX, rChunkZ };
						//Bukkit.broadcastMessage("["+x+","+z+"]->"+arrIndex);//"["+rChunkX+","+rChunkZ+"]");
						//chunksToLoad[chunksToLoad.length - 1] = new int[] { rChunkX, rChunkZ };
//						for (int i = 0; i < DiamondBlockLoc.size(); i++)
//						{
//							Chunk pChunk = p.getWorld().getChunkAt(DiamondBlockLoc.get(i));
//							if (pChunk.getX() == rChunkX && pChunk.getZ() == rChunkZ)
//							{
//								//We can send the block update
//								p.sendBlockChange(DiamondBlockLoc.get(i), Material.STONE.createBlockData());
//							}
//						}
					}
				}
				//Bukkit.broadcastMessage("="+chunksToLoad.length);
				int found = 0;
				for (int i = 0; i < Chunks.size(); i++)
				{
					//Get Indexes and send data
					for (int j = 0; j < chunksToLoad.length; j++)
					{
						//Bukkit.broadcastMessage("="+Chunks.get(i)[0]);
						//Bukkit.broadcastMessage("="+chunksToLoad[j][0]);
						if (chunksToLoad[j][0] == Chunks.get(i)[0] && chunksToLoad[j][1] == Chunks.get(i)[1])
						{
							found++;
							int s = Chunks.get(i)[2];
							int e = Chunks.get(i)[3];
							if (debug) Bukkit.broadcastMessage("Sending " + s + "->" + e);
							List<Location> toSend = DiamondBlockLoc.subList(s, e);
							for (int k = 0; k < toSend.size(); k++)
							{
								p.sendBlockChange(toSend.get(k), Material.STONE.createBlockData());
							}
							break;
						}
					}
					if (found == chunksToLoad.length) break;
				}
			}
		});	
	}
	
	
	private boolean isNewChunk(String player)
	{
		Player p = this.getServer().getPlayer(player);
		if (p == null) return false;
		
		if (!p.isOnline()) return false;
		if (p.getWorld().getEnvironment() != Environment.NORMAL) return false;
		if (!p.getWorld().getName().contentEquals("world")) return false;
		Chunk chunk = p.getWorld().getChunkAt(p.getLocation());
		int i = getPlayerChunkList(player);
		int cChunkX = chunk.getX();
		int cChunkZ = chunk.getZ();
		if (i == -1) { PlayerPrevChunk.add(new Tuple<String, int[]>(player, new int[] {cChunkX, cChunkZ})); return true; }
		int oChunkX = PlayerPrevChunk.get(i).b[0];
		int oChunkZ = PlayerPrevChunk.get(i).b[1];
		boolean t = (cChunkX == oChunkX && cChunkZ == oChunkZ);
		//Bukkit.broadcastMessage("[" + oChunkX + "," + oChunkZ + "]->" + "[" + cChunkX + "," + cChunkZ + "]");
		if (t) return false;
		//Bukkit.broadcastMessage("true");
		PlayerPrevChunk.set(i, new Tuple<String, int[]>(player, new int[] {cChunkX, cChunkZ}));
		return true;
	}
	
	private int getPlayerChunkList(String player)
	{
		for	(int i = 0; i < PlayerPrevChunk.size(); i++)
		{
			if (PlayerPrevChunk.get(i).a.equals(player))
			{
				return i;
			}
		}
		return -1;
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		if (PlayerAffected.contains(event.getPlayer().getName()))
		{
			if (isNewChunk(event.getPlayer().getName()) || Chunks.contains(new int[] { event.getPlayer().getWorld().getChunkAt(event.getPlayer().getLocation()).getX(), event.getPlayer().getWorld().getChunkAt(event.getPlayer().getLocation()).getZ()}))
	    	{
	    		processChunks(scan, event.getPlayer().getName());
	    		if (debug) Bukkit.broadcastMessage("Exec1");
	    	}
			this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			  public void run() {
				  if (debug) Bukkit.broadcastMessage(event.getPlayer().getName());				  
				  if (debug) Bukkit.broadcastMessage("'" + Bukkit.getServer().getPlayer(event.getPlayer().getName()).getWorld().getName() + "', '" + Bukkit.getServer().getPlayer(event.getPlayer().getName()).getWorld().getEnvironment() + "'");
			  	
				  sendValidChunksToPlayer(scan, event.getPlayer().getName());
				  if (debug) Bukkit.broadcastMessage("Exec");
			  }
			}, 1L);
		}
//		int fmi = getFirstMoveIndex(event.getPlayer().getName());
//		if (fmi == -1) { PlayerFirstMove.add(new Tuple<String, Boolean>(event.getPlayer().getName(), true)); return; } //Bukkit.broadcastMessage("HERE"); Bukkit.broadcastMessage("c:" + PlayerFirstMove.size()); return;}
//		PlayerFirstMove.set(fmi, new Tuple<String, Boolean>(event.getPlayer().getName(), true));
	}
	
//	public int getFirstMoveIndex(String player)
//	{
//		for (int i = 0; i < PlayerFirstMove.size(); i++)
//		{
//			if (PlayerFirstMove.get(i).a.equals(player)) return i;
//		}
//		return -1;
//	}
	
	@EventHandler
    public void onMove(PlayerMoveEvent e) {
        String player = e.getPlayer().getName();
        if (PlayerAffected.contains(player))
        {
//        	int fmi = getFirstMoveIndex(player);
//    		if (fmi == -1) 
//    		{
//    			PlayerFirstMove.add(new Tuple<String, Boolean>(player, false)); //This shouldn't happen
//    			fmi = getFirstMoveIndex(player);
//    		}
//    		boolean firstmove = PlayerFirstMove.get(fmi).b;
//        	if (firstmove)
//        	{
//        		Bukkit.broadcastMessage("first move");
//        		PlayerFirstMove.set(fmi, new Tuple<String, Boolean>(player, false));
//        		if (PlayerAffected.contains(player))
//    			{
//    				if (isNewChunk(player))
//    	        	{
//    	        		processChunks(scan, player);
//    	        		
//    	        	}
//    				sendValidChunksToPlayer(scan, player);
//    			}
//        	}
//        	else
//        	{
//        		if (isNewChunk(player))
//            	{
//            		processChunks(scan, player);
//            		sendValidChunksToPlayer(scan, player);
//            	}
//        	}
        	
        	if (isNewChunk(player))
        	{
        		processChunks(scan, player);
        		sendValidChunksToPlayer(scan, player);
        	}
        }
    }
	
	private void indexChunk(Chunk chunk)
	{
		//Check if the diamond block index has a entry for this chunk
//		if (!diamondBlockHasChunk(chunk.getX(), chunk.getZ()))
//		{
//			DiamondBlockLoc.add(new Tuple<int[], List<Location>>(new int[] {chunk.getX(), chunk.getZ()}, new ArrayList<Location>()));
//		}
		int[] chunkid = {chunk.getX(), chunk.getZ()};
		this.getServer().getScheduler().runTask(this, new Runnable() {
			public void run()
			{
				synchronized(DiamondBlockLoc)
				{
					int start = DiamondBlockLoc.size();
					for (int x = 0; x <= 15; x++)
					{
						for (int y = 0; y <= 16; y++)//127; y++) //Diamond only spawns below level 16
						{
							for (int z = 0; z <= 15; z++)
							{
								Block block = chunk.getBlock(x, y, z);
								Material mat = block.getBlockData().getMaterial();
								if (mat.equals(Material.DIAMOND_ORE))
								{
									Location a = block.getLocation();
									//int i = diamondBlockGetChunk(chunk.getX(), chunk.getZ());
									DiamondBlockLoc.add(a);
								}
							}
						}
					}
					int end = DiamondBlockLoc.size();
					int[] chunkDescriptor = { chunk.getX(), chunk.getZ(), start, end};
					Chunks.add(chunkDescriptor);
				}
			}
		});
	}
	
//	private boolean diamondBlockHasChunk(int x, int z)
//	{
//		for (int i = 0; i < DiamondBlockLoc.size(); i++)
//		{
//			int[] cChunkId = DiamondBlockLoc.get(i).a;
//			if (cChunkId[0] == x && cChunkId[1] == z) return true;
//		}
//		return false;
//	}
	
//	private int diamondBlockGetChunk(int x, int z)
//	{
//		for (int i = 0; i < DiamondBlockLoc.size(); i++)
//		{
//			int[] cChunkId = DiamondBlockLoc.get(i).a;
//			if (cChunkId[0] == x && cChunkId[1] == z) return i;
//		}
//		return -1;
//	}
	
	private boolean hasSeenChunk(int x, int z)
	{
		for (int i = 0; i < Chunks.size(); i++)
		{
			if (Chunks.get(i)[0] == x && Chunks.get(i)[1] == z) return true;
		}
		return false;
	}
	
	private void sendBlockChangeToAffected()
	{
		for (Player p:this.getServer().getOnlinePlayers())
		{
			if (PlayerAffected.contains(p.getName()))
			{
				if (debug) Bukkit.broadcastMessage("Sending Block Change to " + p.getName());
				//Send Fake Block Change Events to hide diamond ore
				sendValidChunksToPlayer(scan, p.getName());
			}
		}
	}
	
	@Deprecated
	private void sendBlockChangeToPlayer(Player p)
	{
		if (debug) Bukkit.broadcastMessage("Diamond At: " + DiamondBlockLoc.get(0).getBlockX() + " " + DiamondBlockLoc.get(0).getBlockY() + " " + DiamondBlockLoc.get(0).getBlockZ());
		if (debug) Bukkit.broadcastMessage("Diamond Count: " + DiamondBlockLoc.size());
		for (int i = 0; i < DiamondBlockLoc.size(); i++)
		{
			p.sendBlockChange(DiamondBlockLoc.get(i), Material.STONE.createBlockData());
		}
	}
}
