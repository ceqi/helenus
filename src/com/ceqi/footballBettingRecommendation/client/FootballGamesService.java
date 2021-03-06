package com.ceqi.footballBettingRecommendation.client;

import java.util.ArrayList;

import com.ceqi.footballBettingRecommendation.shared.ScoreWithPos;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("footballGames")
public interface FootballGamesService extends RemoteService {
	ArrayList<String> getGames();

	ArrayList<ScoreWithPos> getPredictions();
}
