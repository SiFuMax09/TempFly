package com.moneybags.tempfly.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.bukkit.command.CommandSender;

import com.moneybags.tempfly.TempFly;
import com.moneybags.tempfly.util.Console;
import com.moneybags.tempfly.util.U;
import com.moneybags.tempfly.util.V;

public abstract class TimeCommand extends TempFlyCommand {
	
	public TimeCommand(TempFly tempfly, String[] args) {
		super(tempfly, args);
	}
	
	public List<String> getPartialUnits(String partial, double val) {
		if (partial == null || partial.isEmpty()) {
			return tempfly.getCommandManager().getAllTimeCompletions();
		}
		List<String> matches = new ArrayList<>();
		units:
		for (String identifier: tempfly.getCommandManager().getAllTimeCompletions()) {
			char[] unitChars = identifier.toCharArray();
			if (partial.length() > unitChars.length) {
				continue;
			}
			for (int i = 0; i < partial.length(); i++) {
				char partialChar = partial.charAt(i);
				if (partialChar != unitChars[i]) {
					continue units;
				}
			}
			matches.add(val != -999 ? String.valueOf(val) + identifier : identifier);
		}
		return matches;
	}
	
	protected boolean isNumeric(String s) {
		return regexNumeric(s).length() == 0;
	}
	
	protected String regexNumeric(String s) {
		return s.replaceAll("[0-9]", "").replaceAll("\\.", "");
	}
	
	protected String regexAlphabetical(String s) {
		return s.replaceAll("[a-zA-Z]", "");
	}
	 
	protected int lastNumericIndex(String s) {
		int fin = 0;
		for (int i = 0; i < s.length(); i++) {
			if (!String.valueOf(s.charAt(i)).matches("[0-9]") && !String.valueOf(s.charAt(i)).equals(".")) {
				return i > 0 ? fin-1 : fin;
			}
			fin++;
		}
		return s.length()-1;
	}
	
	public double quantifyArguments(CommandSender s, int skip) {
		double seconds = 0;
		String[] args = skip > 0 ? cleanArgs(getArguments(), skip) : getArguments();
		for (int i = 0; i < args.length; i++) {
			TimeUnit unit;
			String parse;
			// /tf give -{unit} 1
			if (String.valueOf(args[i].charAt(0)).equals("-")) {
				if ((unit = tempfly.getCommandManager().parseUnit(args[i].toLowerCase().replaceAll("\\-", ""))) == null) {
					U.m(s, U.cc("&c(" + args[i] + ") is an unknown time argument!"));
					return 0;
				}
				if (args.length >= i+1) {
					parse = args[i+1];
					i++;
				} else {
					U.m(s, U.cc("&c(" + args[i] + ") must be followed by a valid number!"));
					return 0;
				}
				
			// /tf give 1{unit}
			} else if (lastNumericIndex(args[i]) < args[i].length()-1) {
				if ((unit = tempfly.getCommandManager().parseUnit(regexNumeric(args[i]).toLowerCase())) == null) {
					U.m(s, U.cc("&c(" + regexNumeric(args[i]) + ") is an unknown time argument!"));
					return 0;
				}
				parse = regexAlphabetical(args[i]);
				
			// /tf give 60 {unit} | /tf give 60
			} else {
				parse = args[i];
				if (args.length-1 > i && (unit = tempfly.getCommandManager().parseUnit(args[i+1])) != null) {
					i++;
				} else {
					unit = TimeUnit.SECONDS;
				}
			}
			try {
				double fin = Double.parseDouble(parse);
				switch (unit) {
				case DAYS: fin *= 24;
				case HOURS: fin *= 60;
				case MINUTES: fin *= 60;
				case SECONDS: seconds += fin;
				default:
					break;
				}
			} catch (NumberFormatException e) {
				U.m(s, V.invalidNumber.replaceAll("\\{NUMBER}", parse));
				return 0;
			}
		}
		return seconds;
	}
	
	public List<String> getTimeArguments(String[] args) {
		if (args == null || args.length == 0 || args.length == 1 && args[0].isEmpty()) {
				return getRange(1, 9);
		}
		
		String lastArg = args[args.length-1];
		// /tf give 1 {unit}
		return ((lastArg.isEmpty() || !isNumeric(lastArg)) && args.length > 1 && isNumeric(args[args.length-2]))
			? getPartialUnits(lastArg, -999) : getRange(1, 9);
		
	}
}
