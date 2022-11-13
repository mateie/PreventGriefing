/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.mateie.preventgriefing.tasks;

import me.mateie.preventgriefing.Messages;
import me.mateie.preventgriefing.PlayerData;
import me.mateie.preventgriefing.PreventGriefing;
import me.mateie.preventgriefing.TextMode;
import org.bukkit.entity.Player;

//sends a message to a player
//used to send delayed messages, for example help text triggered by a player's chat
public class PvPImmunityValidationTask implements Runnable
{
    private final Player player;

    public PvPImmunityValidationTask(Player player)
    {
        this.player = player;
    }

    @Override
    public void run()
    {
        if (!player.isOnline()) return;

        PlayerData playerData = PreventGriefing.instance.dataStore.getPlayerData(player.getUniqueId());
        if (!playerData.pvpImmune) return;

        //check the player's inventory for anything
        if (!PreventGriefing.isInventoryEmpty(player))
        {
            //if found, cancel invulnerability and notify
            playerData.pvpImmune = false;
            PreventGriefing.sendMessage(player, TextMode.Warn, Messages.PvPImmunityEnd);
        }
        else
        {
            //otherwise check again in one minute
            PreventGriefing.instance.getServer().getScheduler().scheduleSyncDelayedTask(PreventGriefing.instance, this, 1200L);
        }
    }
}
