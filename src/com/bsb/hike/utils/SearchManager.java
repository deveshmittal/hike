package com.bsb.hike.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import android.text.TextUtils;

public class SearchManager
{

	private ArrayList<Integer> indexList;

	private String searchText;

	private int itemViewBacklash = 1;

	private ArrayList<Searchable> itemList;

	public SearchManager(Collection<? extends Searchable> collection)
	{
		this.indexList = new ArrayList<Integer>();
		this.itemList = new ArrayList<Searchable>(collection);
	}

	public void makeNewSearch(String s)
	{
		searchText = s;
		indexList.clear();
	}

	public void endSearch()
	{
		makeNewSearch("");
	}

	public String getSearchText()
	{
		return searchText;
	}

	public void setItemViewBacklash(int backlash)
	{
		itemViewBacklash = backlash;
	}

	public int getNextItem(int cursorPosition)
	{
		if (TextUtils.isEmpty(searchText))
		{
			return -1;
		}
		Logger.d("search", "nextMessage()");
		Logger.d("search", "list: " + indexList.toString());
		int currentCusrsor = getCurrentCursor(cursorPosition);
		Logger.d("search", "currentCusrsor: " + currentCusrsor);
		int position = Collections.binarySearch(indexList, currentCusrsor);
		Logger.d("search", "position: " + position);

		int start, end, threshold = -1;
		int lastMessageIndex = itemList.size() - 1;
		if (indexList.isEmpty())
		{
			start = currentCusrsor;
			end = lastMessageIndex;
			threshold = currentCusrsor;
		}
		else
		{
			int lastIndex = indexList.get(indexList.size() - 1);
			int firstIndex = indexList.get(0);
			if (currentCusrsor >= lastIndex)
			{
				start = lastIndex + 1;
				end = lastMessageIndex;
				threshold = currentCusrsor;
			}
			else if (currentCusrsor < firstIndex)
			{
				start = currentCusrsor;
				end = firstIndex - 1;
			}
			else
			{
				if (position >= 0)
				{
					position++;
				}
				else
				{
					position *= (-1);
					position -= 1;
				}
				return applyBackLash(indexList.get(position));
			}
		}
		boolean found = false;
		if (end == lastMessageIndex)
		{
			found = searchFirstMessage(start, end, threshold);
		}
		else
		{
			found = searchAllMessages(start, end);
		}
		position = Collections.binarySearch(indexList, currentCusrsor);
		if (position >= 0)
		{
			position++;
		}
		else
		{
			position *= (-1);
			position -= 1;
		}
		if (position >= 0 && position < indexList.size())
		{
			return applyBackLash(indexList.get(position));
		}
		else
		{
			return -1;
		}
	}

	public int getPrevItem(int cursorPosition)
	{
		if (TextUtils.isEmpty(searchText))
		{
			return -1;
		}
		Logger.d("search", "prevMessage()");
		Logger.d("search", "list: " + indexList.toString());
		int currentCusrsor = getCurrentCursor(cursorPosition);
		Logger.d("search", "currentCusrsor: " + currentCusrsor);
		int position = Collections.binarySearch(indexList, currentCusrsor);
		Logger.d("search", "position: " + position);

		int start, end, threshold = -1;
		if (indexList.isEmpty())
		{
			start = currentCusrsor;
			end = 0;
		}
		else
		{
			int lastIndex = indexList.get(indexList.size() - 1);
			int firstIndex = indexList.get(0);
			if (currentCusrsor > lastIndex)
			{
				start = currentCusrsor;
				end = lastIndex + 1;
				threshold = currentCusrsor;
			}
			else if (currentCusrsor <= firstIndex)
			{
				start = firstIndex - 1;
				end = 0;
				threshold = currentCusrsor;
			}
			else
			{
				if (position >= 0)
				{
					position--;
				}
				else
				{
					position *= (-1);
					position -= 2;
				}
				return applyBackLash(indexList.get(position));
			}
		}
		boolean found = false;
		if (end == 0)
		{
			found = searchFirstMessage(start, end, threshold);
		}
		else
		{
			found = searchAllMessages(start, end);
		}
		position = Collections.binarySearch(indexList, currentCusrsor);
		if (position >= 0)
		{
			position--;
		}
		else
		{
			position *= (-1);
			position -= 2;
		}
		if (position >= 0 && position < indexList.size())
		{
			return applyBackLash(indexList.get(position));
		}
		else
		{
			return -1;
		}
	}

	private int getCurrentCursor(int cursorPosition)
	{
		return (cursorPosition + itemViewBacklash);
	}

	private boolean searchAllMessages(int from, int to)
	{
		boolean found = false;
		if (from > to)
		{
			from ^= to ^= from ^= to;
		}
		for (; from <= to; from++)
		{
			if (itemList.get(from).doesItemContain(searchText))
			{
				Logger.d("search", "adding: " + from);
				indexList.add(from);
				Collections.sort(indexList);
				found = true;
			}
		}
		return found;
	}

	private boolean searchFirstMessage(int from, int to, int threshold)
	{
		Logger.d("search", "searching first in range: " + from + "-" + to + " with threshold of " + threshold);
		if (from > to)
		{
			for (; from >= to; from--)
			{
				if (itemList.get(from).doesItemContain(searchText))
				{
					Logger.d("search", "adding: " + from);
					indexList.add(from);
					Collections.sort(indexList);
					if (threshold >= 0)
					{
						if (from < threshold)
						{
							return true;
						}
					}
					else
					{
						return true;
					}
				}
			}
			return false;
		}
		else
		{
			for (; from <= to; from++)
			{
				if (itemList.get(from).doesItemContain(searchText))
				{
					Logger.d("search", "adding: " + from);
					indexList.add(from);
					Collections.sort(indexList);
					if (threshold >= 0)
					{
						if (from > threshold)
						{
							return true;
						}
					}
					else
					{
						return true;
					}
				}
			}
			return false;
		}
	}

	private int applyBackLash(int position)
	{
		if (position >= itemViewBacklash)
			position -= itemViewBacklash;

		return position;
	}

	public void updateIndex(int count)
	{
		for (Integer index : indexList)
		{
			index += count;
		}
	}

	public interface Searchable
	{
		/**
		 * Checks if the item contains the search text.
		 * 
		 * @return If the item has the search text.
		 */
		boolean doesItemContain(String s);
	}

}
