/*
 * Copyright (C) 2012-2013 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.aggregate.odktables.relation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.opendatakit.aggregate.odktables.rest.TableConstants;
import org.opendatakit.common.ermodel.simple.Entity;
import org.opendatakit.common.ermodel.simple.Query;
import org.opendatakit.common.ermodel.simple.Relation;
import org.opendatakit.common.persistence.CommonFieldsBase;
import org.opendatakit.common.persistence.DataField;
import org.opendatakit.common.persistence.DataField.DataType;
import org.opendatakit.common.persistence.DataField.IndexType;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.common.web.CallingContext;

/**
 * Represents the schema for a user-defined (data, security, shortcut) table
 * in the database.
 * @author dylan price
 * @author sudar.sam@gmail.com
 *
 */
public class DbTable {

  public static final String ROW_VERSION = "ROW_VERSION";
  /**
   * This should hold the data etag at the time the row was modified/created.
   */
  public static final String DATA_ETAG_AT_MODIFICATION = "DATA_ETAG_AT_MODIFICATION";
  public static final String CREATE_USER = "CREATE_USER";
  public static final String LAST_UPDATE_USER = "LAST_UPDATE_USER";
  public static final String FILTER_TYPE = "FILTER_TYPE";
  public static final String FILTER_VALUE = "FILTER_VALUE";
  public static final String DELETED = "DELETED";

  private static final List<DataField> dataFields;
  static {
    dataFields = new ArrayList<DataField>();
    dataFields.add(new DataField(ROW_VERSION, DataType.STRING, false));
    dataFields.add(new DataField(DATA_ETAG_AT_MODIFICATION, DataType.STRING,
        false));
    dataFields.add(new DataField(CREATE_USER, DataType.STRING, true));
    dataFields.add(new DataField(LAST_UPDATE_USER, DataType.STRING, true));
    dataFields.add(new DataField(FILTER_TYPE, DataType.STRING, true));
    dataFields.add(new DataField(FILTER_VALUE, DataType.STRING, true)
    .setIndexable(IndexType.HASH));
    dataFields.add(new DataField(DELETED, DataType.BOOLEAN, false));

    // And now make the OdkTables metadata columns.
    dataFields.add(new DataField(TableConstants.URI_ACCESS_CONTROL.toUpperCase(),
        DataType.STRING, true));
    dataFields.add(new DataField(TableConstants.FORM_ID.toUpperCase(),
        DataType.STRING, true));
    dataFields.add(new DataField(TableConstants.INSTANCE_NAME.toUpperCase(),
        DataType.STRING, true));
    dataFields.add(new DataField(TableConstants.LOCALE.toUpperCase(),
        DataType.STRING, true));
    dataFields.add(new DataField(TableConstants.TIMESTAMP.toUpperCase(),
        DataType.DATETIME, true));
  }

  private static final EntityConverter converter = new EntityConverter();

  public static Relation getRelation(String tableId, CallingContext cc)
      throws ODKDatastoreException {
    List<DataField> fields = getDynamicFields(tableId, cc);
    fields.addAll(getStaticFields());
    return getRelation(tableId, fields, cc);
  }

  private static synchronized Relation getRelation(String tableId, List<DataField> fields,
      CallingContext cc)
      throws ODKDatastoreException {
    Relation relation = new Relation(RUtil.NAMESPACE,
        RUtil.convertIdentifier(tableId), fields, cc);
    return relation;
  }

  private static List<DataField> getDynamicFields(String tableId,
      CallingContext cc)
      throws ODKDatastoreException {
    List<Entity> entities = DbColumnDefinitions.query(tableId, cc);
    return converter.toFields(entities);
  }

  /**
   * This should only be called sparingly.
   * @return
   */
  public static List<DataField> getStaticFields() {
    return Collections.unmodifiableList(dataFields);
  }

  /**
   * Retrieve a list of {@link DbTable} row entities.
   *
   * @param table
   *          the {@link DbTable} relation.
   * @param rowIds
   *          the ids of the rows to get.
   * @param cc
   * @return the row entities
   * @throws ODKEntityNotFoundException
   *           if one of the rows does not exist
   * @throws ODKDatastoreException
   */
  public static List<Entity> query(Relation table, List<String> rowIds,
      CallingContext cc)
      throws ODKEntityNotFoundException, ODKDatastoreException {
    Validate.notNull(table);
    Validate.noNullElements(rowIds);
    Validate.notNull(cc);

    Query query = table.query("DbTable.query", cc);
    query.include(CommonFieldsBase.URI_COLUMN_NAME, rowIds);
    List<Entity> entities = query.execute();
    return entities;
  }

}