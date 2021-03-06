package com.ceqi.footballBettingRecommendation.server.features;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ceqi.footballBettingRecommendation.server.machineLearningModule.Prediction;
import com.ceqi.footballBettingRecommendation.server.rawStats.DiskGameParser;
import com.ceqi.footballBettingRecommendation.server.rawStats.Game;
import com.ceqi.footballBettingRecommendation.server.rawStats.InMemGameParser;
import com.ceqi.footballBettingRecommendation.server.rawStats.RawStats;
import com.google.common.collect.Lists;

/**
 * 
 * Turn one team's raw statistics into features. Features for England Premier
 * League teams.
 * 
 * One Parser object is for one team only.
 * 
 * @author ce
 *
 */

public class EPLFeaturesParser {
	private final int HISTORY;
	// games of one team and no draw
	private List<Game> games = new ArrayList<Game>();
	private EPLTeams team;

	// call fetchTeamData to get THE team's raw statistics
	public EPLFeaturesParser(EPLTeams teamName, List<String> localDatasets,
			int history) {
		this.team = teamName;
		HISTORY = history;
		this.fetchTeamData(localDatasets);
	}

	/**
	 * fetch the raw data, reading season from past to current. starting from
	 * local datasets, then the online datasets.
	 * 
	 * simple CSV file parsing
	 */
	private void fetchRawData(List<String> localDatasets) {

		games = Lists.newArrayList(new DiskGameParser(localDatasets.get(0)));
		for (int i = 1; i < localDatasets.size(); i++)
			games.addAll(Lists.newArrayList(new DiskGameParser(localDatasets
					.get(i))));
		// fetch online data
		games.addAll(Lists.newArrayList(new InMemGameParser()));

	}

	/**
	 * 
	 * Advanced CSV file parsing
	 * 
	 * drop all games but keep a specific team, for example, "Arsenal"; This
	 * will also drop all headlines <br />
	 * drop all draw games the team played;<br />
	 * drop the very last game if it contains empty value (drop line with empty
	 * cells)
	 * 
	 * drop games in such order, if there's empty value contained game, should
	 * be the last one and only one.
	 * 
	 * @return games the team played
	 * 
	 */
	private List<Game> fetchTeamData(List<String> localDatasets) {
		this.fetchRawData(localDatasets);
		Iterator<Game> iteratorAll = games.iterator();
		while (iteratorAll.hasNext()) {
			// drop other teams data, this will also drop headlines, since
			// headlines are treated as 'other team'
			Game game = iteratorAll.next();
			String homeTeam = game.getRawStats().get(
					RawStats.HOMETEAM.toString());
			String awayTeam = game.getRawStats().get(
					RawStats.AWAYTEAM.toString());
			if (!homeTeam.equals(team.toString())
					&& !awayTeam.equals(team.toString()))
				iteratorAll.remove();

		}

		// drop the team's draw data
		Iterator<Game> iterator = games.iterator();
		while (iterator.hasNext()) {
			Game game = iterator.next();
			if (game.getRawStats().get(RawStats.FULLTIME_RESULT.toString())
					.equals("D"))
				iterator.remove();

		}
		// drop last game if it contains empty value, the game has not started
		// yet.
		int lastGameIndex = games.size() - 1;
		if (games.get(lastGameIndex).getRawStats()
				.get(RawStats.FULLTIME_RESULT.toString()).equals("")) {
			games.remove(lastGameIndex);
		}

		return this.games;
	}

	/**
	 * whether a team is playing at home ,1 : is home game, 0 : not home game
	 * 
	 * @return a list of records whether the team played at home
	 */

	public List<String> isHome() {
		List<String> isHomeList = new ArrayList<String>();
		// record isHome data after HISTORY games
		for (int i = HISTORY; i < games.size(); i++) {
			if (this.isTheTeam(homeTeam(i))) {
				isHomeList.add("Y");
			} else
				isHomeList.add("N");
		}
		return isHomeList;
	}

	/**
	 * Average number of points (3 for a win, 0 for a loss) earned in the last
	 * HISTORY games.
	 * 
	 * @return a list of average number of points.
	 */
	public List<Double> avgPoints() {
		List<Double> avgPointsList = new ArrayList<Double>();

		for (int i = 0; i < games.size() - HISTORY; i++) {
			double points = 0, avgPoint = 0;
			for (int j = i; j < i + HISTORY; j++) {
				String result = result(j);
				String homeTeam = homeTeam(j);
				if (this.isTheTeam(homeTeam)) {
					if (result.equals("H"))
						points += 3;
				} else {
					if (result.equals("A"))
						points += 3;
				}

			}
			avgPoint = points / (HISTORY * 90);
			avgPointsList.add(avgPoint);
		}

		return avgPointsList;

	}

	/**
	 * opponent's average number of points (3 for a win, 0 for a loss) earned in
	 * the last HISTORY games.
	 * 
	 * @return a list of points
	 */

	public List<Double> opAvgPoints() {
		List<Double> opAvgPointsList = new ArrayList<Double>();
		for (int i = 0; i < games.size() - HISTORY; i++) {
			double points = 0, opAvgPoint = 0;
			for (int j = i; j < i + HISTORY; j++) {
				String result = result(j);
				String homeTeam = homeTeam(j);
				if (this.isTheTeam(homeTeam)) {
					if (result.equals("A"))
						points += 3;
				} else {
					if (result.equals("H"))
						points += 3;
				}
			}
			opAvgPoint = points / (HISTORY * 90);
			opAvgPointsList.add(opAvgPoint);

		}
		return opAvgPointsList;
	}

	/**
	 * Average number of goals scored in the last HISTORY games.
	 * 
	 * @return a list of average goals
	 */
	public List<Double> avgGoals() {
		List<Double> avgGoalsList = new ArrayList<Double>();
		for (int i = 0; i < games.size() - HISTORY; i++) {
			double goals = 0, avgGoal = 0;
			for (int j = i; j < i + HISTORY; j++) {
				String homeTeam = homeTeam(j);
				Double ftAwayGoal = awayGoals(j);
				Double ftHomeGoal = homeGoals(j);
				if (this.isTheTeam(homeTeam))
					goals += ftHomeGoal;
				else
					goals += ftAwayGoal;
			}
			avgGoal = goals / (HISTORY * 90);
			avgGoalsList.add(avgGoal);
		}
		return avgGoalsList;
	}

	/**
	 * opponent's average number of goals scored in the last HISTORY games
	 * 
	 * @return
	 */
	public List<Double> opAvgGoals() {
		List<Double> opAvgGoalsList = new ArrayList<Double>();
		for (int i = 0; i < games.size() - HISTORY; i++) {

			double goals = 0, opAvgGoals = 0;
			for (int j = i; j < i + HISTORY; j++) {
				String homeTeam = homeTeam(j);
				Double ftAwayGoal = awayGoals(j);
				Double ftHomeGoal = homeGoals(j);
				if (this.isTheTeam(homeTeam)) {
					goals += ftAwayGoal;
				} else {
					goals += ftHomeGoal;
				}
			}
			opAvgGoals = goals / (HISTORY * 90);
			opAvgGoalsList.add(opAvgGoals);
		}

		return opAvgGoalsList;
	}

	/**
	 *
	 * Number of corner kicks awarded per minute
	 * 
	 * @return
	 */

	public List<Double> corners() {
		List<Double> cornersList = new ArrayList<Double>();
		for (int i = 0; i < games.size() - HISTORY; i++) {
			double total = 0, corners = 0;
			for (int j = i; j < i + HISTORY; j++) {
				String homeTeam = homeTeam(j);
				Double hc = homeCorners(j);
				Double ac = awayCorners(j);
				if (this.isTheTeam(homeTeam))
					total += hc;
				else
					total += ac;
			}
			corners = total / (HISTORY * 90);
			cornersList.add(corners);
		}
		return cornersList;
	}

	/**
	 * opponent corners
	 * 
	 * @return
	 */
	public List<Double> opCorners() {
		List<Double> opCornersList = new ArrayList<Double>();
		for (int i = 0; i < games.size() - HISTORY; i++) {
			double total = 0, opCorners = 0;
			for (int j = i; j < i + HISTORY; j++) {
				String homeTeam = homeTeam(j);
				Double hc = homeCorners(j);
				Double ac = awayCorners(j);
				if (this.isTheTeam(homeTeam))
					total += ac;
				else
					total += hc;
			}
			opCorners = total / (HISTORY * 90);
			opCornersList.add(opCorners);
		}
		return opCornersList;
	}

	/**
	 * fouls: Number of fouls committed per minute/10 ( 6 seconds)
	 * 
	 * @return fouls committed per minute/10 ( 6 seconds).
	 **/
	public List<Double> fouls() {
		List<Double> foulsList = new ArrayList<Double>();
		for (int i = 0; i < games.size() - HISTORY; i++) {
			double total = 0, fouls = 0;
			for (int j = i; j < i + HISTORY; j++) {
				String homeTeam = homeTeam(j);
				Double hf = homeFouls(j);
				Double af = awayFouls(j);
				if (this.isTheTeam(homeTeam))
					total += hf;
				else
					total += af;
			}
			fouls = total / (HISTORY * 90 * 10);
			foulsList.add(fouls);
		}
		return foulsList;
	}

	/**
	 * number of fouls committed by opponents per minute.
	 * 
	 * @return fouls committed by opponents per minute/10 ( 6 seconds).
	 */
	public List<Double> opFouls() {
		List<Double> opFoulsList = new ArrayList<Double>();
		for (int i = 0; i < games.size() - HISTORY; i++) {
			double total = 0, opFouls = 0;
			for (int j = i; j < i + HISTORY; j++) {
				String homeTeam = homeTeam(j);
				Double hf = homeFouls(j);
				Double af = awayFouls(j);
				if (this.isTheTeam(homeTeam))
					total += af;
				else
					total += hf;
			}
			opFouls = total / (HISTORY * 90 * 10);
			opFoulsList.add(opFouls);
		}
		return opFoulsList;
	}

	/**
	 * cards: Number of yellow and red cards received per minute
	 * 
	 */

	public List<Double> cards() {
		List<Double> cardsList = new ArrayList<Double>();
		for (int i = 0; i < games.size() - HISTORY; i++) {
			double total = 0, cards = 0;
			for (int j = i; j < i + HISTORY; j++) {
				String homeTeam = homeTeam(j);
				double homeCards = homeCards(j);
				double awayCards = awayCards(j);
				if (this.isTheTeam(homeTeam))
					total = total + homeCards;
				else
					total = total + awayCards;
			}
			cards = total / (HISTORY * 90);
			cardsList.add(cards);
		}
		return cardsList;

	}

	/**
	 * opponent's cards received per minute
	 */
	public List<Double> opCards() {
		List<Double> opCardsList = new ArrayList<Double>();
		for (int i = 0; i < games.size() - HISTORY; i++) {
			double total = 0, opCards = 0;
			for (int j = i; j < i + HISTORY; j++) {
				String homeTeam = homeTeam(j);
				double homeCards = homeCards(j);
				double awayCards = awayCards(j);
				if (this.isTheTeam(homeTeam))
					total = total + awayCards;
				else
					total = total + homeCards;
			}
			opCards = total / (HISTORY * 90);
			opCardsList.add(opCards);
		}
		return opCardsList;

	}

	/**
	 * shots: Number of shots taken per minute/10 (6 seconds).
	 * 
	 * @return
	 */
	public List<Double> shots() {
		List<Double> shotsList = new ArrayList<Double>();
		for (int i = 0; i < games.size() - HISTORY; i++) {
			double total = 0, shots = 0;
			for (int j = i; j < i + HISTORY; j++) {
				String homeTeam = homeTeam(j);
				Double hs = homeShots(j);
				Double as = awayShots(j);
				if (this.isTheTeam(homeTeam))
					total += hs;
				else
					total += as;
			}
			shots = total / (HISTORY * 90 * 10);
			shotsList.add(shots);
		}
		return shotsList;

	}

	/**
	 * opponent's shots taken per minute/10 (6 seconds).
	 */
	public List<Double> opShots() {
		List<Double> opShotsList = new ArrayList<Double>();
		for (int i = 0; i < games.size() - HISTORY; i++) {
			double total = 0, opShots = 0;
			for (int j = i; j < i + HISTORY; j++) {
				String homeTeam = homeTeam(j);
				Double hs = homeShots(j);
				Double as = awayShots(j);
				if (this.isTheTeam(homeTeam))
					total += as;
				else
					total += hs;
			}
			opShots = total / (HISTORY * 90 * 10);
			opShotsList.add(opShots);
		}
		return opShotsList;
	}

	/**
	 * number of shots on target taken per minute.
	 * 
	 * @return
	 */
	public List<Double> shotsOnTarget() {
		List<Double> shotsOnTargetList = new ArrayList<Double>();
		for (int i = 0; i < games.size() - HISTORY; i++) {
			double total = 0, shotsOnTarget = 0;
			for (int j = i; j < i + HISTORY; j++) {
				String homeTeam = homeTeam(j);
				Double hst = homeShotsOnTarget(j);
				Double ast = awayShotsOnTarget(j);
				if (this.isTheTeam(homeTeam))
					total += hst;
				else
					total += ast;
			}
			shotsOnTarget = total / (HISTORY * 90);
			shotsOnTargetList.add(shotsOnTarget);
		}
		return shotsOnTargetList;
	}

	/**
	 * opponent's number of shots on target taken per minute
	 */
	public List<Double> opShotsOnTarget() {
		List<Double> opShotsOnTargetList = new ArrayList<Double>();
		for (int i = 0; i < games.size() - HISTORY; i++) {
			double total = 0, opShotsOnTarget = 0;
			for (int j = i; j < i + HISTORY; j++) {
				String homeTeam = homeTeam(j);
				Double hst = homeShotsOnTarget(j);
				Double ast = awayShotsOnTarget(j);
				if (this.isTheTeam(homeTeam))
					total += ast;
				else
					total += hst;
			}
			opShotsOnTarget = total / (HISTORY * 90);
			opShotsOnTargetList.add(opShotsOnTarget);
		}
		return opShotsOnTargetList;
	}

	/**
	 * Full time results for the team
	 */
	public List<Integer> ftResults() {
		List<Integer> ftResultsList = new ArrayList<Integer>();
		for (int i = HISTORY; i < games.size(); i++) {
			String homeTeam = homeTeam(i);
			String fulltimeResult = result(i);
			Integer ftResult;
			if (this.isTheTeam(homeTeam)) {
				if (fulltimeResult.equals("H"))
					ftResult = 1;
				else
					ftResult = 0;
			} else {
				if (fulltimeResult.equals("A"))
					ftResult = 1;
				else
					ftResult = 0;
			}
			ftResultsList.add(ftResult);
		}
		return ftResultsList;
	}

	/*
	 * methods with Ins suffix are tested in Instance test class generate
	 * instance features using last HISTORY games raw statistics
	 */

	/**
	 * Instance(for prediction) of a team is playing at home or away.
	 * 
	 * @return a team is playing at home or away.
	 */
	public String isHomeIns() {

		return Prediction.getIsHomeMap().get(team);
	}

	/**
	 * instance(for prediction) average points
	 * 
	 * @return the average points
	 */
	public double avgPointsIns() {
		double points = 0, avgPointsIns = 0;
		for (int i = games.size() - HISTORY; i < games.size(); i++) {
			String result = result(i);
			String homeTeam = homeTeam(i);
			if (this.isTheTeam(homeTeam)) {
				if (result.equals("H"))
					points += 3;
			} else {
				if (result.equals("A"))
					points += 3;
			}
		}
		avgPointsIns = points / (HISTORY * 90);
		return avgPointsIns;
	}

	/**
	 * instance(for prediction) opponent average points gained
	 * 
	 * @return the opponent average points
	 */
	public double opAvgPointsIns() {
		double points = 0, opAvgPointsIns = 0;
		for (int i = games.size() - HISTORY; i < games.size(); i++) {
			String result = result(i);
			String homeTeam = homeTeam(i);
			// opponent wins
			if (this.isTheTeam(homeTeam)) {

				if (result.equals("A")) {
					points += 3;
				}
			} else {
				if (result.equals("H")) {
					points += 3;
				}
			}
		}
		opAvgPointsIns = points / (HISTORY * 90);
		return opAvgPointsIns;
	}

	/**
	 * instance(for prediction) average goals gained
	 * 
	 * @return the average goals
	 */
	public double avgGoalsIns() {
		double goals = 0, avgGoalsIns = 0;
		for (int i = games.size() - HISTORY; i < games.size(); i++) {
			String homeTeam = homeTeam(i);
			double homeGoals = homeGoals(i);
			double awayGoals = awayGoals(i);
			// get the team's goals
			if (this.isTheTeam(homeTeam)) {
				goals += homeGoals;
			} else {
				goals += awayGoals;
			}
		}
		avgGoalsIns = goals / (HISTORY * 90);
		return avgGoalsIns;
	}

	/**
	 * instance (for prediction) opponent average goals gained.
	 * 
	 * @return opponent average goals
	 */
	public double opAvgGoalsIns() {
		double opGoals = 0, opAvgGoalsIns = 0;
		for (int i = games.size() - HISTORY; i < games.size(); i++) {
			String homeTeam = homeTeam(i);
			double homeGoals = homeGoals(i);
			double awayGoals = awayGoals(i);
			// get the opponents of the team's goals
			if (this.isTheTeam(homeTeam)) {
				opGoals += awayGoals;
			} else {
				opGoals += homeGoals;
			}
		}
		opAvgGoalsIns = opGoals / (HISTORY * 90);
		return opAvgGoalsIns;
	}

	/**
	 * instance (for prediction) corners gained per minute
	 * 
	 * @return corners awarded per minute
	 */
	public double cornersIns() {
		double totalCorners = 0, corners = 0;
		for (int i = games.size() - HISTORY; i < games.size(); i++) {
			String homeTeam = homeTeam(i);
			double homeCorners = homeCorners(i);
			double awayCorners = awayCorners(i);
			// get the team's corners
			if (this.isTheTeam(homeTeam)) {
				totalCorners += homeCorners;
			} else {
				totalCorners += awayCorners;
			}
		}
		corners = totalCorners / (HISTORY * 90);
		return corners;
	}

	/**
	 * instance (for prediction) opponents corners gained per minute
	 * 
	 * @return opponents' corners awarded per minute
	 */
	public double opCornersIns() {
		double totalOpCorners = 0, opCorners = 0;
		for (int i = games.size() - HISTORY; i < games.size(); i++) {
			String homeTeam = homeTeam(i);
			double homeCorners = homeCorners(i);
			double awayCorners = awayCorners(i);
			// get opponents' corners
			if (this.isTheTeam(homeTeam)) {
				totalOpCorners += awayCorners;
			} else {
				totalOpCorners += homeCorners;
			}
		}
		opCorners = totalOpCorners / (HISTORY * 90);
		return opCorners;
	}

	/**
	 * instance (for prediction) number of fouls committed per minute.
	 * 
	 * @return fouls committed per minute/10 ( 6 seconds).
	 * 
	 */
	public double foulsIns() {
		double total = 0, fouls = 0;
		for (int i = games.size() - HISTORY; i < games.size(); i++) {
			String homeTeam = homeTeam(i);
			double hf = homeFouls(i);
			double af = awayFouls(i);
			// get fouls committed by the team
			if (this.isTheTeam(homeTeam)) {
				total += hf;
			} else {
				total += af;
			}
		}
		fouls = total / (HISTORY * 90 * 10);
		return fouls;
	}

	/**
	 * instance (for prediction) number of fouls committed by opponents per
	 * minute.
	 * 
	 * @return fouls committed by opponents per minute/10 ( 6 seconds).
	 * 
	 */
	public double opFoulsIns() {
		double opTotal = 0, opFouls = 0;
		for (int i = games.size() - HISTORY; i < games.size(); i++) {
			String homeTeam = homeTeam(i);
			double hf = homeFouls(i);
			double af = awayFouls(i);
			// get fouls committed by opponents.
			if (this.isTheTeam(homeTeam)) {
				opTotal += af;
			} else {
				opTotal += hf;
			}
		}
		opFouls = opTotal / (HISTORY * 90 * 10);
		return opFouls;
	}

	/**
	 * instance (for prediction) cards committed per minute.
	 * 
	 * @return cards committed per minute.
	 */
	public double cardsIns() {
		double total = 0, cards = 0;
		for (int i = games.size() - HISTORY; i < games.size(); i++) {
			String homeTeam = homeTeam(i);
			double homeCards = homeCards(i);
			double awayCards = awayCards(i);

			// get cards committed by the team
			if (this.isTheTeam(homeTeam)) {
				total = total + homeCards;
			} else {
				total = total + awayCards;
			}
		}
		cards = total / (HISTORY * 90);
		return cards;
	}

	/**
	 * instance (for prediction) cards committed by opponents per minute.
	 * 
	 * @return opponents cards committed per minute.
	 */
	public double opCardsIns() {
		double opTotal = 0, opCards = 0;
		for (int i = games.size() - HISTORY; i < games.size(); i++) {
			String homeTeam = homeTeam(i);
			double homeCards = homeCards(i);
			double awayCards = awayCards(i);
			// get cards committed by opponents
			if (this.isTheTeam(homeTeam)) {
				opTotal = opTotal + awayCards;
			} else {
				opTotal = opTotal + homeCards;
			}
		}
		opCards = opTotal / (HISTORY * 90);
		return opCards;
	}

	/**
	 * instance (for prediction) number of shots taken per minute/10 (6
	 * seconds).
	 * 
	 * @return shots taken per minute/10 (6 seconds).
	 */
	public double shotsIns() {
		double total = 0, shots = 0;
		for (int i = games.size() - HISTORY; i < games.size(); i++) {

			String homeTeam = homeTeam(i);
			double hs = homeShots(i);
			double as = awayShots(i);
			// get shots taken by the team
			if (this.isTheTeam(homeTeam)) {
				total += hs;
			} else {
				total += as;
			}
		}
		shots = total / (HISTORY * 90 * 10);
		return shots;
	}

	/**
	 * instance (for prediction) number of shots taken by opponents per
	 * minute/10 (6 seconds).
	 * 
	 * @return opponents shots taken per minute/10 (6 seconds).
	 */
	public double opShotsIns() {
		double opTotal = 0, opShots = 0;
		for (int i = games.size() - HISTORY; i < games.size(); i++) {
			String homeTeam = homeTeam(i);
			double hs = homeShots(i);
			double as = awayShots(i);
			// get shots taken by opponents
			if (this.isTheTeam(homeTeam)) {
				opTotal += as;
			} else {
				opTotal += hs;
			}
		}
		opShots = opTotal / (HISTORY * 90 * 10);
		return opShots;
	}

	/**
	 * instance (for prediction) number of shots on target taken per minute.
	 * 
	 * @return shots on target taken per minute.
	 */
	public double shotsOnTargetIns() {
		double total = 0, shotsOnTarget = 0;
		for (int i = games.size() - HISTORY; i < games.size(); i++) {
			String homeTeam = homeTeam(i);
			double hst = homeShotsOnTarget(i);
			double ast = awayShotsOnTarget(i);
			// get shots on target committed by the team
			if (this.isTheTeam(homeTeam)) {
				total += hst;
			} else {
				total += ast;
			}

		}
		shotsOnTarget = total / (HISTORY * 90);
		return shotsOnTarget;
	}

	/**
	 * instance (for prediction) number of shots on target taken by opponents
	 * per minute.
	 * 
	 * @return opponents shots on target taken per minute.
	 */
	public double opShotsOnTargetIns() {
		double opTotal = 0, opShotsOnTarget = 0;
		for (int i = games.size() - HISTORY; i < games.size(); i++) {
			String homeTeam = homeTeam(i);
			double hst = homeShotsOnTarget(i);
			double ast = awayShotsOnTarget(i);
			// get shots on target comitted by opponents.
			if (this.isTheTeam(homeTeam)) {
				opTotal += ast;
			} else {
				opTotal += hst;
			}
		}
		opShotsOnTarget = opTotal / (HISTORY * 90);
		return opShotsOnTarget;
	}

	/*
	 * ---------------------------------miscellaneous----------------------------
	 */
	public List<Game> getGames() {
		return this.games;
	}

	/*
	 * check if parameter team is the team.
	 */
	public boolean isTheTeam(String teamName) {
		if (this.team.toString().equals(teamName)) {
			return true;
		} else
			return false;
	}

	/**
	 * The i-th game's homeTeam in past HISTORY games.
	 * 
	 * @param i
	 *            game index
	 * @return team name of HomeTeam
	 */
	public String homeTeam(int i) {
		return games.get(i).getRawStats().get(RawStats.HOMETEAM.toString());
	}

	/**
	 * The i-th game's full time result during past HISTORY games.
	 * 
	 * @param i
	 *            game index
	 * @return full time result
	 */
	public String result(int i) {
		return games.get(i).getRawStats()
				.get(RawStats.FULLTIME_RESULT.toString());
	}

	/**
	 * The i-th game's goals scored by homeTeam during past HISTORY games.
	 * 
	 * @param i
	 *            game index
	 * @return goals scored by home team
	 */
	public double homeGoals(int i) {
		return Double.valueOf(games.get(i).getRawStats()
				.get(RawStats.FULLTIME_HOMEGOALS.toString()));

	}

	/**
	 * The i-th game's goals scored by away team during past HISTORY games.
	 * 
	 * @param i
	 *            game index
	 * @return goals scored by away team
	 */
	public double awayGoals(int i) {
		return Double.valueOf(games.get(i).getRawStats()
				.get(RawStats.FULLTIME_AWAYGOALS.toString()));
	}

	/**
	 * The i-th game's corners awarded to home team during past HISTORY games.
	 * 
	 * @param i
	 *            game index
	 * @return corners awarded to home team
	 */
	public double homeCorners(int i) {
		return Double.valueOf(games.get(i).getRawStats()
				.get(RawStats.HOME_CORNERS.toString()));

	}

	/**
	 * The i-th game's corners awarded to away team during past HISTORY games.
	 * 
	 * @param i
	 *            game index
	 * @return corners awarded to away team
	 */
	public double awayCorners(int i) {
		return Double.valueOf(games.get(i).getRawStats()
				.get(RawStats.AWAY_CORNERS.toString()));
	}

	/**
	 * The i-th game's homeTeam shots in past HISTORY games.
	 * 
	 * @param i
	 *            game index
	 * @return shots committed by home team.
	 */
	public double homeShots(int i) {
		return Double.valueOf(games.get(i).getRawStats()
				.get(RawStats.HOME_SHOTS.toString()));
	}

	/**
	 * The i-th game's away team shots in past HISTORY games.
	 * 
	 * @param i
	 *            game index
	 * @return shots committed by away team.
	 */
	public double awayShots(int i) {
		return Double.valueOf(games.get(i).getRawStats()
				.get(RawStats.AWAY_SHOTS.toString()));
	}

	/**
	 * The i-th game's home team shots on target in past HISTORY games.
	 * 
	 * @param i
	 *            game index
	 * @return shots on target committed by home team.
	 */
	public double homeShotsOnTarget(int i) {
		double hst = Double.valueOf(games.get(i).getRawStats()
				.get(RawStats.HOME_SHOTS_ON_TARGET.toString()));

		return hst;
	}

	/**
	 * The i-th game's away team shots on target in past HISTORY games.
	 * 
	 * @param i
	 *            game index
	 * @return shots on target committed by away team.
	 */
	public double awayShotsOnTarget(int i) {
		double ast = Double.valueOf(games.get(i).getRawStats()
				.get(RawStats.AWAY_SHOTS_ON_TARGET.toString()));
		return ast;
	}

	/**
	 * The i-th game's fouls committed by home team in past HISTORY games.
	 * 
	 * @param i
	 *            game index
	 * @return fouls committed by home team
	 */
	public double homeFouls(int i) {
		return Double.valueOf(games.get(i).getRawStats()
				.get(RawStats.HOME_FOULS.toString()));

	}

	/**
	 * The i-th game's fouls committed by away team in past HISTORY games.
	 * 
	 * @param i
	 *            game index
	 * @return fouls committed by away team
	 */
	public double awayFouls(int i) {
		return Double.valueOf(games.get(i).getRawStats()
				.get(RawStats.AWAY_FOULS.toString()));
	}

	/**
	 * The i-th game's cards shown to home team in past HISTORY games.
	 * 
	 * @param i
	 *            game index
	 * @return cards shown to home team
	 */
	public double homeCards(int i) {
		double hy = Double.valueOf(games.get(i).getRawStats()
				.get(RawStats.HOME_YELLOW_CARDS.toString()));
		double hr = Double.valueOf(games.get(i).getRawStats()
				.get(RawStats.HOME_RED_CARDS.toString()));

		return hy + hr;
	}

	/**
	 * The i-th game's cards shown to away team in past HISTORY games.
	 * 
	 * @param i
	 *            game index
	 * @return cards shown to away team
	 */
	public double awayCards(int i) {
		double ay = Double.valueOf(games.get(i).getRawStats()
				.get(RawStats.AWAY_YELLOW_CARDS.toString()));
		double ar = Double.valueOf(games.get(i).getRawStats()
				.get(RawStats.AWAY_RED_CARDS.toString()));
		return ay + ar;
	}

}
