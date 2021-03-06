package com.ceqi.footballBettingRecommendation.server.scheduledTask;

import static java.util.concurrent.TimeUnit.HOURS;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;

import com.ceqi.footballBettingRecommendation.server.machineLearningModule.Prediction;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class UpdateControl extends RemoteServiceServlet {
	private final ScheduledExecutorService scheduler = Executors
			.newScheduledThreadPool(1, new DaemonThreadFactory());
	private URL url;
	private InputStream oldInStream = null;
	private InputStream latestInputStream = null;
	// to know how many times the data has updated
	private int count = 0;

	public UpdateControl() {
		try {
			url = new URL("http://www.football-data.co.uk/mmz4281/1516/E0.csv");
			oldInStream = url.openStream();
		} catch (IOException e) {
			throw new RuntimeException("Error reading data", e);
		}
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		System.out.println("start checking....");
		scheduleGamesAndScores();

	}

	public void scheduleGamesAndScores() {
		final Runnable fetchGamesAndScores = new Runnable() {

			@Override
			public void run() {
				try {
					latestInputStream = url.openStream();
					// update prediction if football-data.co.uk's data updated
					// or generate predictions for the first time
					if (count == 0
							|| !IOUtils.contentEquals(oldInStream,
									latestInputStream)) {
						Prediction.init();
						Prediction.generatePredictions();
						Prediction.fillInNotAvailableTeamScores();
					}
					oldInStream = latestInputStream;
					count++;
				} catch (IOException e) {
					throw new RuntimeException("Error reading data", e);
				}

			}

		};

		// run scheduled task every day
		scheduler.scheduleAtFixedRate(fetchGamesAndScores, 0, 24, HOURS);
	}
}
