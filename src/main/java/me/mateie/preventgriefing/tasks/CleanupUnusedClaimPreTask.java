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

import me.mateie.preventgriefing.Claim;
import me.mateie.preventgriefing.CustomLogEntryTypes;
import me.mateie.preventgriefing.PlayerData;
import me.mateie.preventgriefing.PreventGriefing;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

//asynchronously loads player data without caching it in the datastore, then
//passes those data to a claim cleanup task which might decide to delete a claim for inactivity

class CleanupUnusedClaimPreTask implements Runnable
{
    private UUID ownerID = null;

    CleanupUnusedClaimPreTask(UUID uuid)
    {
        this.ownerID = uuid;
    }

    @Override
    public void run()
    {
        //get the data
        PlayerData ownerData = PreventGriefing.instance.dataStore.getPlayerDataFromStorage(ownerID);
        OfflinePlayer ownerInfo = Bukkit.getServer().getOfflinePlayer(ownerID);

        PreventGriefing.AddLogEntry("Looking for expired claims.  Checking data for " + ownerID.toString(), CustomLogEntryTypes.Debug, true);

        //expiration code uses last logout timestamp to decide whether to expire claims
        //don't expire claims for online players
        if (ownerInfo.isOnline())
        {
            PreventGriefing.AddLogEntry("Player is online. Ignoring.", CustomLogEntryTypes.Debug, true);
            return;
        }
        if (ownerInfo.getLastPlayed() <= 0)
        {
            PreventGriefing.AddLogEntry("Player is new or not in the server's cached userdata. Ignoring. getLastPlayed = " + ownerInfo.getLastPlayed(), CustomLogEntryTypes.Debug, true);
            return;
        }

        //skip claims belonging to exempted players based on block totals in config
        int bonusBlocks = ownerData.getBonusClaimBlocks();
        if (bonusBlocks >= PreventGriefing.instance.config_claims_expirationExemptionBonusBlocks || bonusBlocks + ownerData.getAccruedClaimBlocks() >= PreventGriefing.instance.config_claims_expirationExemptionTotalBlocks)
        {
            PreventGriefing.AddLogEntry("Player exempt from claim expiration based on claim block counts vs. config file settings.", CustomLogEntryTypes.Debug, true);
            return;
        }

        Claim claimToExpire = null;

        for (Claim claim : PreventGriefing.instance.dataStore.getClaims())
        {
            if (ownerID.equals(claim.ownerID))
            {
                claimToExpire = claim;
                break;
            }
        }

        if (claimToExpire == null)
        {
            PreventGriefing.AddLogEntry("Unable to find a claim to expire for " + ownerID.toString(), CustomLogEntryTypes.Debug, false);
            return;
        }

        //pass it back to the main server thread, where it's safe to delete a claim if needed
        Bukkit.getScheduler().scheduleSyncDelayedTask(PreventGriefing.instance, new CleanupUnusedClaimTask(claimToExpire, ownerData, ownerInfo), 1L);
    }
}