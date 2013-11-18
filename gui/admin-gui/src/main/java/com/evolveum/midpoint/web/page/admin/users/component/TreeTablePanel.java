/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgTableDto;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgTreeDto;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.ISortableTreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.TableTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.TreeColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.WindowsTheme;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.*;

/**
 * @author lazyman
 */
public class TreeTablePanel extends SimplePanel<String> {

    private static final Trace LOGGER = TraceManager.getTrace(TreeTablePanel.class);

    private static final String ID_TREE = "tree";
    private static final String ID_TABLE = "table";
    private static final String ID_FORM = "form";
    private IModel<OrgTreeDto> selected = new LoadableModel<OrgTreeDto>() {

        @Override
        protected OrgTreeDto load() {
            return loadRoot();
        }
    };

    public TreeTablePanel(String id, IModel<String> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        ISortableTreeProvider provider = new OrgTreeProvider(this, getModel());
        List<IColumn<OrgTreeDto, String>> columns = new ArrayList<IColumn<OrgTreeDto, String>>();
        columns.add(new TreeColumn<OrgTreeDto, String>(createStringResource("TreeTablePanel.hierarchy")));

        TableTree<OrgTreeDto, String> tree = new TableTree<OrgTreeDto, String>(ID_TREE, columns, provider,
                Integer.MAX_VALUE, new TreeStateModel()) {

            @Override
            protected Component newContentComponent(String id, IModel<OrgTreeDto> model) {
                return new SelectableFolderContent(id, this, model, selected) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        super.onClick(target);

                        selectTreeItemPerformed(target);
                    }
                };
            }

            @Override
            protected Item<OrgTreeDto> newRowItem(String id, int index, final IModel<OrgTreeDto> model) {
                Item item = super.newRowItem(id, index, model);
                item.add(AttributeModifier.append("class", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        OrgTreeDto itemObject = model.getObject();
                        if (itemObject != null && itemObject.equals(selected.getObject())) {
                            return "success";
                        }

                        return null;
                    }
                }));
                return item;
            }
        };
        tree.getTable().addTopToolbar(new HeadersToolbar<String>(tree.getTable(), null));
        tree.getTable().add(AttributeModifier.replace("class", "table table-striped table-condensed"));
        tree.add(new WindowsTheme());
        add(tree);

        Form form = new Form(ID_FORM);
        add(form);
        BaseSortableDataProvider tableProvider = new ObjectDataProvider<OrgTableDto, ObjectType>(this, ObjectType.class) {

            @Override
            public OrgTableDto createDataObjectWrapper(PrismObject<ObjectType> obj) {
                return OrgTableDto.createDto(obj);
            }

            @Override
            public ObjectQuery getQuery() {
                ObjectQuery query = super.getQuery();
                if (query == null) {
                    query = createTableQuery();
                }
                return query;
            }
        };
        List<IColumn<OrgTableDto, String>> tableColumns = createTableColumns();
        TablePanel table = new TablePanel(ID_TABLE, tableProvider, tableColumns, 10);
        table.setOutputMarkupId(true);
        form.add(table);
    }

    private OrgTreeDto loadRoot() {
        TableTree<OrgTreeDto, String> tree = (TableTree) get(ID_TREE);
        ITreeProvider<OrgTreeDto> provider = tree.getProvider();
        Iterator<? extends OrgTreeDto> iterator = provider.getRoots();

        return iterator.hasNext() ? iterator.next() : null;
    }

    private List<IColumn<OrgTableDto, String>> createTableColumns() {
        List<IColumn<OrgTableDto, String>> columns = new ArrayList<IColumn<OrgTableDto, String>>();

        columns.add(new CheckBoxHeaderColumn<OrgTableDto>());
        columns.add(new IconColumn<OrgTableDto>(createStringResource("")) {

            @Override
            protected IModel<String> createIconModel(IModel<OrgTableDto> rowModel) {
                OrgTableDto dto = rowModel.getObject();

                ObjectTypeGuiDescriptor descr = ObjectTypeGuiDescriptor.getDescriptor(dto.getType());
                String icon = descr != null ? descr.getIcon() : ObjectTypeGuiDescriptor.ERROR_ICON;

                return new Model(icon);
            }
        });

        columns.add(new LinkColumn<OrgTableDto>(createStringResource("ObjectType.name"), OrgTableDto.F_NAME, "name") {

            @Override
            public boolean isEnabled(IModel<OrgTableDto> rowModel) {
                OrgTableDto dto = rowModel.getObject();
                return UserType.class.equals(dto.getType()) || OrgType.class.equals(dto.getType());
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<OrgTableDto> rowModel) {
                OrgTableDto dto = rowModel.getObject();
                if (UserType.class.equals(dto.getType())) {
                    PageParameters parameters = new PageParameters();
                    parameters.add(PageUser.PARAM_USER_ID, dto.getOid());
                    setResponsePage(PageUser.class, parameters);
                } else if (OrgType.class.equals(dto.getType())) {
                    PageParameters parameters = new PageParameters();
                    parameters.add(PageOrgUnit.PARAM_ORG_ID, dto.getOid());
                    setResponsePage(PageOrgUnit.class, parameters);
                }
            }
        });
        columns.add(new PropertyColumn<OrgTableDto, String>(createStringResource("OrgType.displayName"), OrgTableDto.F_DISPLAY_NAME));
        //todo add relation
        columns.add(new PropertyColumn<OrgTableDto, String>(createStringResource("OrgType.identifier"), OrgTableDto.F_IDENTIFIER));
        columns.add(new InlineMenuHeaderColumn(initInlineMenu()));

        return columns;
    }

    private List<InlineMenuItem> initInlineMenu() {
        List<InlineMenuItem> headerMenuItems = new ArrayList<InlineMenuItem>();
        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.addOrgUnit"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        addOrgUnit(target);
                    }
                }));
        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.deleteOrgUnit"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        deleteOrgUnit(target);
                    }
                }));
        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.move"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        move(target);
                    }
                }));

        return headerMenuItems;
    }

    private void move(AjaxRequestTarget target) {
        //todo implement [lazyman]
    }

    private void addOrgUnit(AjaxRequestTarget target) {
        //todo implement [lazyman]
    }

    private void deleteOrgUnit(AjaxRequestTarget target) {
        //todo implement [lazyman]
    }

    private void selectTreeItemPerformed(AjaxRequestTarget target) {
        TablePanel table = (TablePanel) get(ID_TABLE);

        BaseSortableDataProvider provider = (BaseSortableDataProvider) table.getDataTable().getDataProvider();
        provider.setQuery(createTableQuery());

        target.add(table);
    }

    private ObjectQuery createTableQuery() {
        OrgTreeDto dto = selected.getObject();
        String oid = dto != null ? dto.getOid() : getModel().getObject();

        OrgFilter filter = OrgFilter.createOrg(oid, null, 1);
        return ObjectQuery.createObjectQuery(filter);
    }

    private static class TreeStateModel extends AbstractReadOnlyModel<Set<OrgTreeDto>> {

        private Set<OrgTreeDto> set = new HashSet<OrgTreeDto>();

        @Override
        public Set<OrgTreeDto> getObject() {
            return set;
        }
    }
}