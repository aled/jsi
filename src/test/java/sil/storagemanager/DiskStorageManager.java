// Spatial Index Library
//
// Copyright (C) 2002  Navel Ltd.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// Contact information:
//  Mailing address:
//    Marios Hadjieleftheriou
//    University of California, Riverside
//    Department of Computer Science
//    Surge Building, Room 310
//    Riverside, CA 92521
//
//  Email:
//    marioh@cs.ucr.edu

package sil.storagemanager;

import java.util.*;
import java.io.*;

public class DiskStorageManager implements IStorageManager
{
	private RandomAccessFile m_dataFile = null;
	private RandomAccessFile m_indexFile = null;
	private int m_pageSize = 0;
	private int m_nextPage = -1;
	private TreeSet m_emptyPages = new TreeSet();
	private HashMap m_pageIndex = new HashMap();
	private byte[] m_buffer = null;

	public DiskStorageManager(PropertySet ps)
		throws SecurityException, NullPointerException, IOException, FileNotFoundException, IllegalArgumentException
	{
		Object var;

		// Open/Create flag.
		boolean bOverwrite = false;
		var = ps.getProperty("Overwrite");

		if (var != null)
		{
			if (! (var instanceof Boolean)) throw new IllegalArgumentException("Property Overwrite must be a Boolean");
			bOverwrite = ((Boolean) var).booleanValue();
		}

		// storage filename.
		var = ps.getProperty("FileName");

		if (var != null)
		{
			if (! (var instanceof String)) throw new IllegalArgumentException("Property FileName must be a String");

			File indexFile = new File((String) var + ".idx");
			File dataFile = new File((String) var + ".dat");

			// check if files exist.
			if (bOverwrite == false && (! indexFile.exists() || ! dataFile.exists())) bOverwrite = true;

			if (bOverwrite)
			{
				if (indexFile.exists()) indexFile.delete();
				if (dataFile.exists()) dataFile.delete();

				boolean b = indexFile.createNewFile();
				if (b == false) throw new IOException("Index file cannot be opened.");

				b = dataFile.createNewFile();
				if (b == false) throw new IOException("Data file cannot be opened.");
			}

			m_indexFile = new RandomAccessFile(indexFile, "rw");
			m_dataFile = new RandomAccessFile(dataFile, "rw");
		}
		else
		{
			throw new IllegalArgumentException("Property FileName was not specified.");
		}

		// find page size.
		if (bOverwrite == true)
		{
			var = ps.getProperty("PageSize");

			if (var != null)
			{
				if (! (var instanceof Integer)) throw new IllegalArgumentException("Property PageSize must be an Integer");
				m_pageSize = ((Integer) var).intValue();
				m_nextPage = 0;
			}
			else
			{
				throw new IllegalArgumentException("Property PageSize was not specified.");
			}
		}
		else
		{
			try
			{
				m_pageSize = m_indexFile.readInt();
			}
			catch (EOFException ex)
			{
				throw new IllegalStateException("Failed reading pageSize.");
			}

			try
			{
				m_nextPage = m_indexFile.readInt();
			}
			catch (EOFException ex)
			{
				throw new IllegalStateException("Failed reading nextPage.");
			}
		}

		// create buffer.
		m_buffer = new byte[m_pageSize];

		if (bOverwrite == false)
		{
			int count, id, page;

			// load empty pages in memory.
			try
			{
				count = m_indexFile.readInt();

				for (int cCount = 0; cCount < count; cCount++)
				{
					page = m_indexFile.readInt();
					m_emptyPages.add(new Integer(page));
				}

				// load index table in memory.
				count = m_indexFile.readInt();

				for (int cCount = 0; cCount < count; cCount++)
				{
					Entry e = new Entry();

					id = m_indexFile.readInt();
					e.m_length = m_indexFile.readInt();

					int count2 = m_indexFile.readInt();

					for (int cCount2 = 0; cCount2 < count2; cCount2++)
					{
						page = m_indexFile.readInt();
						e.m_pages.add(new Integer(page));
					}
					m_pageIndex.put(new Integer(id), e);
				}
			}
			catch (EOFException ex)
			{
				throw new IllegalStateException("Corrupted index file.");
			}
		}
	}

	public void flush()
	{
		try
		{
			m_indexFile.seek(0l);

			m_indexFile.writeInt(m_pageSize);
			m_indexFile.writeInt(m_nextPage);

			int id, page;
			int count = m_emptyPages.size();

			m_indexFile.writeInt(count);

			Iterator it = m_emptyPages.iterator();
			while (it.hasNext())
			{
				page = ((Integer) it.next()).intValue();
				m_indexFile.writeInt(page);
			}

			count = m_pageIndex.size();
			m_indexFile.writeInt(count);

			it = m_pageIndex.entrySet().iterator();

			while (it.hasNext())
			{
				Map.Entry me = (Map.Entry) it.next();
				id = ((Integer) me.getKey()).intValue();
				m_indexFile.writeInt(id);

				Entry e = (Entry) me.getValue();
				count = e.m_length;
				m_indexFile.writeInt(count);

				count = e.m_pages.size();
				m_indexFile.writeInt(count);

				for (int cIndex = 0; cIndex < count; cIndex++)
				{
					page = ((Integer) e.m_pages.get(cIndex)).intValue();
					m_indexFile.writeInt(page);
				}
			}
		}
		catch (IOException ex)
		{
			throw new IllegalStateException("Corrupted index file.");
		}
	}

	public byte[] loadByteArray(final int id)
	{
		Entry e = (Entry) m_pageIndex.get(new Integer(id));
		if (e == null) throw new InvalidPageException(id);

		int cNext = 0;
		int cTotal = e.m_pages.size();

		byte[] data = new byte[e.m_length];
		int cIndex = 0;
		int cLen;
		int cRem = e.m_length;

		do
		{
			try
			{
				m_dataFile.seek(((Integer) e.m_pages.get(cNext)).intValue() * m_pageSize);
				int bytesread = m_dataFile.read(m_buffer);
				if (bytesread != m_pageSize) throw new IllegalStateException("Corrupted data file.");
			}
			catch (IOException ex)
			{
				throw new IllegalStateException("Corrupted data file.");
			}

			cLen = (cRem > m_pageSize) ? m_pageSize : cRem;
			System.arraycopy(m_buffer, 0, data, cIndex, cLen);

			cIndex += cLen;
			cRem -= cLen;
			cNext++;
		}
		while (cNext < cTotal);

		return data;
	}

	public int storeByteArray(final int id, final byte[] data)
	{
		if (id == NewPage)
		{
			Entry e = new Entry();
			e.m_length = data.length;

			int cIndex = 0;
			int cPage;
			int cRem = data.length;
			int cLen;

			while (cRem > 0)
			{
				if (! m_emptyPages.isEmpty())
				{
					Integer i = (Integer) m_emptyPages.first();
					m_emptyPages.remove(i);
					cPage = i.intValue();
				}
				else
				{
					cPage = m_nextPage;
					m_nextPage++;
				}

				cLen = (cRem > m_pageSize) ? m_pageSize : cRem;
				System.arraycopy(data, cIndex, m_buffer, 0, cLen);

				try
				{
					m_dataFile.seek(cPage * m_pageSize);
					m_dataFile.write(m_buffer);
				}
				catch (IOException ex)
				{
					throw new IllegalStateException("Corrupted data file.");
				}

				cIndex += cLen;
				cRem -= cLen;
				e.m_pages.add(new Integer(cPage));
			}

			Integer i = (Integer) e.m_pages.get(0);
			m_pageIndex.put(i, e);

			return i.intValue();
		}
		else
		{
			// find the entry.
			Entry oldEntry = (Entry) m_pageIndex.get(new Integer(id));
			if (oldEntry == null) throw new InvalidPageException(id);

			m_pageIndex.remove(new Integer(id));

			Entry e = new Entry();
			e.m_length = data.length;

			int cIndex = 0;
			int cPage;
			int cRem = data.length;
			int cLen, cNext = 0;

			while (cRem > 0)
			{
				if (cNext < oldEntry.m_pages.size())
				{
					cPage = ((Integer) oldEntry.m_pages.get(cNext)).intValue();
					cNext++;
				}
				else if (! m_emptyPages.isEmpty())
				{
					Integer i = (Integer) m_emptyPages.first();
					m_emptyPages.remove(i);
					cPage = i.intValue();
				}
				else
				{
					cPage = m_nextPage;
					m_nextPage++;
				}

				cLen = (cRem > m_pageSize) ? m_pageSize : cRem;
				System.arraycopy(data, cIndex, m_buffer, 0, cLen);

				try
				{
					m_dataFile.seek(cPage * m_pageSize);
					m_dataFile.write(m_buffer);
				}
				catch (IOException ex)
				{
					throw new IllegalStateException("Corrupted data file.");
				}

				cIndex += cLen;
				cRem -= cLen;
				e.m_pages.add(new Integer(cPage));
			}

			while (cNext < oldEntry.m_pages.size())
			{
				m_emptyPages.add(oldEntry.m_pages.get(cNext));
				cNext++;
			}

			Integer i = (Integer) e.m_pages.get(0);
			m_pageIndex.put(i, e);

			return i.intValue();
		}
	}

	public void deleteByteArray(final int id)
	{
		// find the entry.
		Entry e = (Entry) m_pageIndex.get(new Integer(id));
		if (e == null) throw new InvalidPageException(id);

		m_pageIndex.remove(new Integer(id));

		for (int cIndex = 0; cIndex < e.m_pages.size(); cIndex++)
		{
			m_emptyPages.add(e.m_pages.get(cIndex));
		}
	}

	public void close()
	{
		flush();
	}

	class Entry
	{
		int m_length = 0;
		ArrayList m_pages = new ArrayList();
	}
}
