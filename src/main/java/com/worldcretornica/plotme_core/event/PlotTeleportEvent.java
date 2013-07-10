package com.worldcretornica.plotme_core.event;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import com.worldcretornica.plotme_core.Plot;
import com.worldcretornica.plotme_core.PlotMe_Core;

public class PlotTeleportEvent extends PlotEvent implements Cancellable
{
	private static final HandlerList handlers = new HandlerList();
    private boolean _canceled;
    private Plot _plot;
    private World _world;
    private Player _player;
    private String _plotid;
    private Location _loc;
	
    public PlotTeleportEvent(PlotMe_Core instance, World world, Plot plot, Player player, Location loc, String plotId)
    {
    	super(instance);
    	_plot = plot;
    	_world = world;
    	_player = player;
    	_loc = loc;
    	_plotid = plotId;
    }
    
	@Override
	public boolean isCancelled() 
	{
		return _canceled;
	}

	@Override
	public void setCancelled(boolean cancel) 
	{
		_canceled = cancel;
	}

	@Override
	public HandlerList getHandlers() 
	{
		return handlers;
	}
	
	public static HandlerList getHandlerList() 
	{
        return handlers;
    }
	
	public Plot getPlot()
	{
		return _plot;
	}
	
	public World getWorld()
	{
		return _world;
	}
	
	public Player getPlayer()
	{
		return _player;
	}
	
	public Location getLocation()
	{
		return _loc;
	}
	
	public String getPlotId()
	{
		return _plotid;
	}
	
	public boolean getIsPlotClaimed()
	{
		return (_plot != null);
	}
		
	public String getOwner()
	{
		if(_plot != null)
			return _plot.owner;
		else
			return "";
	}
	
	public Location getUpperBound()
	{
		return plugin.getPlotMeCoreManager().getGenMan(_world).getPlotTopLoc(_world, _plotid);
	}
	
	public Location getLowerBound()
	{
		return plugin.getPlotMeCoreManager().getGenMan(_world).getPlotBottomLoc(_world, _plotid);
	}
}
