/**
 * 
 */
package com.ceqi.footballBettingRecommendation.server.rawStats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;

/**
 * simple csv file parsing parse CSV file line by line<br />
 * parse headline once only. <br />
 * ignore lines with empty cells.
 * 
 * @author ce
 *
 */
public class DiskGameParser extends AbstractGameParser implements
		Iterable<Game> {

	private String resourceName;

	public DiskGameParser(String resourceName) {
		URL url = Resources.getResource(resourceName);

		if (url != null) {
			this.resourceName = resourceName;
		}

	}

	@Override
	public Iterator<Game> iterator() {
		try {
			return new AbstractIterator<Game>() {
				BufferedReader input = new BufferedReader(
						new InputStreamReader(Resources.getResource(
								resourceName).openStream()));

				// read field names ONCE only
				Iterable<String> fieldNames = onComma.split(input.readLine());

				@Override
				protected Game computeNext() {
					try {
						String line = input.readLine();
						if (line == null)
							return endOfData();
						// ignore parsing headLine
						if (line.contains("HomeTeam"))
							return null;

						Iterables.limit(onComma.split(line), limitSize);

						return new Game(fieldNames, onComma.split(line),
								limitSize);

					} catch (IOException e) {
						throw new RuntimeException("Error reading data", e);
					}

				}
			};
		} catch (IOException e) {
			throw new RuntimeException("Error reading data", e);
		}
	}
}
