/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.action.autonaming;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;

public class DefaultAutoNamingRule implements AutoNamingRule {

	private final String suffixSeparator;

	private final String branchSeparator;

	private final boolean incrementSuffix;

	private final Pattern branchPattern;

	public DefaultAutoNamingRule() {
		this(".", "", true);
	}

	public DefaultAutoNamingRule(final String suffixSeparator, final String branchSeparator, final boolean incrementSuffix) {
		this.suffixSeparator = suffixSeparator;
		this.branchSeparator = branchSeparator;
		this.incrementSuffix = incrementSuffix;
		this.branchPattern = Pattern.compile("^([a-z](?:" + Pattern.quote(branchSeparator) + "[a-z])*)$");
	}

	@Override
	public void nameRoot(final Spot root, final TrackModel model) {
		final Integer id = model.trackIDOf(root);
		final String trackName = model.name(id);

		final String rootName = (incrementSuffix)
				? trackName + suffixSeparator + "1"
				: trackName;
		root.setName(rootName);
	}

	@Override
	public void nameBranches(final Spot mother, final Collection<Spot> siblings) {
		// Sort siblings by their X position.
		final List<Spot> spots = sortSiblingsByXPosition(siblings);

		// Predecessor name.
		final String motherName = mother.getName();
		final String[] tokens = motherName.split(Pattern.quote(suffixSeparator));

		// Find in what token the branch suffix is stored.
		int branchTokenIndex = findBranchTokenIndex(tokens);

		if (branchTokenIndex < 0) {
			// Could not find branch token; add new branch suffix.
			addNewBranchSuffix(motherName, spots);
			return;
		}

		// Add a new branch name for each sibling.
		addBranchNames(tokens, spots, branchTokenIndex);
	}

	private List<Spot> sortSiblingsByXPosition(final Collection<Spot> siblings) {
		final List<Spot> spots = new ArrayList<>(siblings);
		spots.sort(Comparator.comparing(s -> s.getDoublePosition(0)));
		return spots;
	}

	private int findBranchTokenIndex(final String[] tokens) {
		for (int i = 0; i < tokens.length; i++) {
			final String token = tokens[i];
			final Matcher matcher = branchPattern.matcher(token);
			if (matcher.matches()) {
				return i;
			}
		}
		return -1;
	}

	private void addNewBranchSuffix(final String motherName, final List<Spot> spots) {
		char bname = 'a';
		for (final Spot spot : spots) {
			spot.setName(motherName + suffixSeparator + bname);
			bname += 1;
		}
	}

	private void addBranchNames(final String[] tokens, final List<Spot> spots, final int branchTokenIndex) {
		char bname = 'a';
		for (final Spot spot : spots) {
			final String[] newTokens = Arrays.copyOf(tokens, tokens.length);
			if (!incrementSuffix) {
				newTokens[newTokens.length - 1] += branchSeparator + bname;
			} else {
				newTokens[newTokens.length - 2] += branchSeparator + bname;
				newTokens[newTokens.length - 1] = "1"; // Restart.
			}
			spot.setName(String.join(suffixSeparator, newTokens));
			bname += 1;
		}
	}

	@Override
	public void nameSpot(final Spot current, final Spot predecessor) {
		if (incrementSuffix) {
			final String name = predecessor.getName();
			final String[] tokens = name.split(Pattern.quote(suffixSeparator));
			final String idstr = tokens[tokens.length - 1];
			try {
				final Integer id = Integer.valueOf(idstr);
				tokens[tokens.length - 1] = Integer.toString(id + 1);
				final String name2 = String.join(suffixSeparator, tokens);
				current.setName(name2);

			} catch (final NumberFormatException e) {
				AutoNamingRule.super.nameSpot(current, predecessor);
			}
		} else {
			AutoNamingRule.super.nameSpot(current, predecessor);
		}
	}

	@Override
	public String getInfoText() {
		final StringBuilder str = new StringBuilder();
		str.append("<html>");
		str.append(generateDescriptionText());
		str.append("<ul>");
		str.append(generateRootNamingRuleText());
		str.append(generateBranchNamingRuleText());
		if (!branchSeparator.isEmpty()) {
			str.append(generateBranchSeparatorRuleText());
		}
		if (!suffixSeparator.isEmpty()) {
			str.append(generateSuffixSeparatorRuleText());
		}
		if (incrementSuffix) {
			str.append(generateIncrementSuffixRuleText());
		}
		str.append("</ul>");
		str.append(generateExampleDescriptionText());
		str.append("</html>");
		return str.toString();
	}

	// Helper Methods for Explaining Variables
	private String generateDescriptionText() {
		return "Rename all the spots in a model giving to daughter branches a name derived "
				+ "from the mother branch. The daughter branch names are determined "
				+ "following simple rules based on the X position of spots:";
	}

	private String generateRootNamingRuleText() {
		return "<li>The root (first spot) of a track takes the name of the track it belongs to.</li>";
	}

	private String generateBranchNamingRuleText() {
		return "<li>The subsequent branches are named from the mother branch they split from. Their name "
				+ "is suffixed by 'a', 'b', ... depending on the relative X position of the sibling spots "
				+ "just after division.</li>";
	}

	private String generateBranchSeparatorRuleText() {
		return "<li>Each of the branch character is separated from others by the character '"
				+ branchSeparator + "'</li>";
	}

	private String generateSuffixSeparatorRuleText() {
		return "<li>The branch suffix ('a' ...) is separated from the root name by the character '"
				+ suffixSeparator + "'</li>";
	}

	private String generateIncrementSuffixRuleText() {
		return "<li>Inside a branch, the individual spots are suffixed by a supplemental index ('1', '2', ...), "
				+ "indicating their order in the branch.</li>";
	}

	private String generateExampleDescriptionText() {
		final StringBuilder exampleBuilder = new StringBuilder();
		exampleBuilder.append("<p>");
		exampleBuilder.append("For instance, the 3rd spot of the branch following two divisions, first one emerging "
				+ "from the leftmost sibling and second one emerging from the rightmost sibling, in  "
				+ "the track named 'Track_23' will be named: <br>");
		String example = "Track_23" + suffixSeparator + "a" + branchSeparator + "b";
		if (incrementSuffix) {
			example += suffixSeparator + "3";
		}
		exampleBuilder.append("<div style='text-align: center;'>" + example + "<br>");
		exampleBuilder.append("The results are undefined if a track is not a tree (if it has merge points).");
		return exampleBuilder.toString();
	}
}