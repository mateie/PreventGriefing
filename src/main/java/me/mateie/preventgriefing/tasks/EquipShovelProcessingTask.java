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

import com.preventgriefing.visualization.BoundaryVisualization;
import com.preventgriefing.visualization.VisualizationType;
import me.mateie.preventgriefing.Claim;
import me.mateie.preventgriefing.ClaimPermission;
import me.mateie.preventgriefing.DataStore;
import me.mateie.preventgriefing.Messages;
import me.mateie.preventgriefing.PlayerData;
import me.mateie.preventgriefing.PreventGriefing;
import me.mateie.preventgriefing.ShovelMode;
import me.mateie.preventgriefing.TextMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

//tells a player about how many claim blocks he has, etc
//implemented as a task so that it can be delayed
//otherwise, it's spammy when players mouse-wheel past the shovel in their hot bars
public class EquipShovelProcessingTask implements Runnable
{
    //player data
    private final Player player;

    public EquipShovelProcessingTask(Player player)
    {
        this.player = player;
    }

    @Override
    public void run()
    {
        //if he's not holding the golden shovel anymore, do nothing
        if (PreventGriefing.instance.getItemInHand(player, EquipmentSlot.HAND).getType() != PreventGriefing.instance.config_claims_modificationTool)
            return;

        PlayerData playerData = PreventGriefing.instance.dataStore.getPlayerData(player.getUniqueId());

        //reset any work he might have been doing
        playerData.lastShovelLocation = null;
        playerData.claimResizing = null;

        //always reset to basic claims mode
        if (playerData.shovelMode != ShovelMode.Basic)
        {
            playerData.shovelMode = ShovelMode.Basic;
            PreventGriefing.sendMessage(player, TextMode.Info, Messages.ShovelBasicClaimMode);
        }

        //tell him how many claim blocks he has available
        int remainingBlocks = playerData.getRemainingClaimBlocks();
        PreventGriefing.sendMessage(player, TextMode.Instr, Messages.RemainingBlocks, String.valueOf(remainingBlocks));

        //link to a video demo of land claiming, based on world type
        if (PreventGriefing.instance.creativeRulesApply(player.getLocation()))
        {
            PreventGriefing.sendMessage(player, TextMode.Instr, Messages.CreativeBasicsVideo2, DataStore.CREATIVE_VIDEO_URL);
        }
        else if (PreventGriefing.instance.claimsEnabledForWorld(player.getWorld()))
        {
            PreventGriefing.sendMessage(player, TextMode.Instr, Messages.SurvivalBasicsVideo2, DataStore.SURVIVAL_VIDEO_URL);
        }

        //if standing in a claim owned by the player, visualize it
        Claim claim = PreventGriefing.instance.dataStore.getClaimAt(player.getLocation(), true, playerData.lastClaim);
        if (claim != null && claim.checkPermission(player, ClaimPermission.Edit, null) == null)
        {
            playerData.lastClaim = claim;
            BoundaryVisualization.visualizeClaim(player, claim, VisualizationType.CLAIM);
        }
    }
}
