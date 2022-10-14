package dansplugins.factionsystem.commands.abs;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

/*public class TabCompleterBase implements TabCompleter {
	private PersistentData persistentData;
	private ConfigService configService;

	public TabCompleterBase(PersistentData persistentData, ConfigService configService) {
		this.persistentData = persistentData;
		this.configService = configService;
	}

	public  List<String> getOnlinePlayers(String partialName) {
		return filterStartingWith(partialName, Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getName));
	}

	public String joinArgsBeyond(int index, String delim, String[] args) {
		++index;
		String[] data = new String[args.length - index];
		System.arraycopy(args, index, data, 0, data.length);
		return String.join(delim, data);
	}

	public List<String> filterStartingWith(String prefix, Stream<String> stream) {
		return stream.filter((s) -> {
			return s != null && !s.isEmpty() && s.toLowerCase().startsWith(prefix.toLowerCase());
		}).collect(Collectors.toList());
	}

	public List<String> filterStartingWith(String prefix, Collection<String> strings) {
		return filterStartingWith(prefix, strings.stream());
	}

	public List<String> tackOnBeginningAndEndQuotes(List<String> targetedList) {
		final List<String> changed = new ArrayList<>();
		for(String string : targetedList) {
			String changedString = '"' + string + '"';
			changed.add(changedString);
		}

		return changed;
	}

	public String removeBeginningAndEndQuotes(String targetedString) {
		return targetedString.replace("\"", "");
	}

	List<String> argsLength1 = new ArrayList<String>();

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		final List<String> factionsAllowedtoAlly = new ArrayList<>();
		final List<String> factionsAllowedtoWar = new ArrayList<>();
		final List<String> officersInFaction = new ArrayList<>();
		final List<String> membersInFaction = new ArrayList<>();
		final List<String> factionNames = new ArrayList<>();
		List<String> result = new ArrayList<String>();

		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (argsLength1.isEmpty()) {
				argsLength1.addAll(Arrays.asList(
						"force",
				));
			}
			if (args.length == 2) {
				if (args[0].equalsIgnoreCase("force")) {
					if(sender.hasPermission("mf.force.*")) {
						return filterStartingWith(args[1], Arrays.asList("save", "load", "peace", "demote", "join", "kick", "power", "renounce", "transfer", "removevassal", "rename", "bonuspower", "unlock", "create", "claim", "flag"));
					}
				}
				return null;
			}

			if (args.length == 3) {

				if (args[0].equalsIgnoreCase("force")) {
					if (args[1].equalsIgnoreCase("peace")) {
						persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
						return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(factionNames));
					}
					if (args[1].equalsIgnoreCase("demote")) {
						return filterStartingWith(args[2], getOnlinePlayers(args[2]));
					}
					if (args[1].equalsIgnoreCase("join")) {
						return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(getOnlinePlayers(args[2])));
					}
					if (args[1].equalsIgnoreCase("kick")) {
						return filterStartingWith(args[2], getOnlinePlayers(args[2]));
					}
					if (args[1].equalsIgnoreCase("power")) {
						return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(getOnlinePlayers(args[2])));
					}
					if (args[1].equalsIgnoreCase("renounce")) {
						if (persistentData.isInFaction(player.getUniqueId())) {
							persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
							return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(factionNames));
						}
					}
					if (args[1].equalsIgnoreCase("transfer")) {
						if (persistentData.isInFaction(player.getUniqueId())) {
							persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
							return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(factionNames));
						}
					}
					if (args[1].equalsIgnoreCase("removevassal")) {
						persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
						return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(factionNames));
					}
					if (args[1].equalsIgnoreCase("rename")) {
						persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
						return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(factionNames));
					}
					if (args[1].equalsIgnoreCase("bonuspower")) {
						persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
						return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(factionNames));
					}
					if (args[1].equalsIgnoreCase("unlock")) {
						return filterStartingWith(args[2], Collections.singletonList("cancel"));
					}
					if (args[1].equalsIgnoreCase("claim")) {
						persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
						return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(factionNames));
					}
					if (args[1].equalsIgnoreCase("flag")) {
						persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
						return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(factionNames));
					}
				}
				return null;
			}

			if (args.length == 4) {
				if (args[0].equalsIgnoreCase("force")) {
					if (args[1].equalsIgnoreCase("peace")) {
						persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
						return filterStartingWith(args[3], tackOnBeginningAndEndQuotes(factionNames));
					}
					if (args[1].equalsIgnoreCase("join")) {
						persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
						return filterStartingWith(args[3], tackOnBeginningAndEndQuotes(factionNames));
					}
					if (args[1].equalsIgnoreCase("transfer")) {
						persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
						return filterStartingWith(args[3], tackOnBeginningAndEndQuotes(getOnlinePlayers(args[2])));
					}
					if (args[1].equalsIgnoreCase("removevassal")) {
						if(persistentData.getFaction(removeBeginningAndEndQuotes(args[2])) != null) {
							Faction faction = persistentData.getFaction(removeBeginningAndEndQuotes(args[2]));
							return filterStartingWith(args[3], tackOnBeginningAndEndQuotes(faction.getVassals()));
						}
					}
					if (args[1].equalsIgnoreCase("flag")) {
						if (persistentData.getFaction(removeBeginningAndEndQuotes(args[2])) != null) {
							Faction faction = persistentData.getFaction(removeBeginningAndEndQuotes(args[2]));
							return filterStartingWith(args[3], faction.getFlags().getFlagNamesList());
						}
					}
				}
				return null;
			}
		}

		return null;
	}
}
*/