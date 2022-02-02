/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.entando.entando.plugins.jpseo.aps.system.services.mapping;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import com.agiletec.aps.system.common.AbstractSearcherDAO;
import com.agiletec.aps.system.common.FieldSearchFilter;
import org.entando.entando.ent.exception.EntException;

/**
 * @author E.Santoboni
 */
public class SeoMappingDAO extends AbstractSearcherDAO implements ISeoMappingDAO {

	private static final EntLogger _logger =  EntLogFactory.getSanitizedLogger(SeoMappingDAO.class);
    
    private static final String TABLE_NAME = "jpseo_friendlycode";

	private static final String ADD_MAPPING = 
			"INSERT INTO " + TABLE_NAME + " (friendlycode, pagecode, contentid, langcode) VALUES (?, ?, ?, ?)";
	
	private static final String LOAD_MAPPINGS = 
			"SELECT friendlycode, pagecode, contentid, langcode FROM " + TABLE_NAME;
    
    private static final String DELETE_FROM_CONTENTID = "DELETE FROM " + TABLE_NAME + " WHERE contentid = ?";
    
    private static final String DELETE_FROM_PAGECODE = "DELETE FROM " + TABLE_NAME + " WHERE pagecode = ?";

	private static final String DELETE_FROM_FRIENDLYCODE = "DELETE FROM " + TABLE_NAME + " WHERE friendlycode = ?";

	@Override
	public Map<String, FriendlyCodeVO> loadMapping() {
		Map<String, FriendlyCodeVO> mapping = new HashMap<>();
		Connection conn = null;
		Statement stat = null;
		ResultSet res = null;
		try {
			conn = this.getConnection();
			stat = conn.createStatement();
			res = stat.executeQuery(LOAD_MAPPINGS);
			FriendlyCodeVO vo = null;
			while (res.next()) {
				vo = new FriendlyCodeVO();
				vo.setFriendlyCode(res.getString(1));
				vo.setPageCode(res.getString(2));
				vo.setContentId(res.getString(3));
				vo.setLangCode(res.getString(4));
				mapping.put(vo.getFriendlyCode(), vo);
			}
		} catch (Throwable t) {
			_logger.error("Error while loading mapping",  t);
			throw new RuntimeException("Error while loading mapping", t);
		} finally {
			closeDaoResources(res, stat, conn);
		}
		return mapping;
	}
	
	@Override
	public void updateMapping(FriendlyCodeVO vo) {
		Connection conn = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
            super.executeQueryWithoutResultset(conn, DELETE_FROM_FRIENDLYCODE, vo.getFriendlyCode());
			this.addRecord(vo, conn);
			conn.commit();
		} catch (Throwable t) {
			this.executeRollback(conn);
			_logger.error("Error update the mapping",  t);
			throw new RuntimeException("Error update the mapping", t);
		} finally {
			this.closeConnection(conn);
		}
	}
	
	@Override
	public void updateMapping(ContentFriendlyCode contentFriendlyCode) {
		PreparedStatement stat = null;
		Connection conn = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
            super.executeQueryWithoutResultset(conn, DELETE_FROM_CONTENTID, contentFriendlyCode.getContentId());
			stat = conn.prepareStatement(ADD_MAPPING);
			String contentId = contentFriendlyCode.getContentId();
			Iterator<Entry<String, String>> codes = contentFriendlyCode.getFriendlyCodes().entrySet().iterator();
			while (codes.hasNext()) {
				Entry<String, String> currentCode = codes.next();
				stat.setString(1, currentCode.getValue());
				stat.setString(2, null);
				stat.setString(3, contentId);
				stat.setString(4, currentCode.getKey());
				stat.addBatch();
				stat.clearParameters();
			}
			stat.executeBatch();
			conn.commit();
		} catch (Throwable t) {
			this.executeRollback(conn);
			_logger.error("Error update the content mapping", t);
			throw new RuntimeException("Error update the content mapping", t);
		} finally {
			this.closeDaoResources(null, stat, conn);
		}
	}
	
	protected void addRecord(FriendlyCodeVO vo, Connection conn) throws EntException {
		PreparedStatement stat = null;
		try {
			stat = conn.prepareStatement(ADD_MAPPING);
			stat.setString(1, vo.getFriendlyCode());
			stat.setString(2, vo.getPageCode());
			stat.setString(3, vo.getContentId());
			stat.setString(4, vo.getLangCode());
			stat.executeUpdate();
		} catch (Throwable t) {
			_logger.error("Error adding a record", t);
			throw new RuntimeException("Error adding a record", t);
		} finally {
			closeDaoResources(null, stat);
		}
	}
    
    @Override
    public void deleteMappingForContent(String contentId) {
        super.executeQueryWithoutResultset(DELETE_FROM_CONTENTID, contentId);
    }

    @Override
    public void deleteMappingForPage(String pageCode) {
        super.executeQueryWithoutResultset(DELETE_FROM_PAGECODE, pageCode);
    }
    
	@Override
	public List<String> searchFriendlyCode(FieldSearchFilter[] filters) {
		return super.searchId(filters);
	}

	@Override
	protected String getTableFieldName(String metadataFieldKey) {
		return metadataFieldKey;
	}
	
	@Override
	protected String getMasterTableName() {
		return TABLE_NAME;
	}
	
	@Override
	protected String getMasterTableIdFieldName() {
		return "friendlycode";
	}
	
}