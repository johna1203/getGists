package com.kodokux.github.ui;

import com.intellij.find.FindProgressIndicator;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Factory;
import com.intellij.spellchecker.ui.SpellCheckingEditorCustomization;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.EditorCustomization;
import com.intellij.ui.EditorTextFieldProvider;
import com.intellij.ui.SoftWrapsEditorCustomization;
import com.intellij.ui.components.JBList;
import com.intellij.usages.*;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: johna
 * Date: 13/05/05
 * Time: 23:59
 * To change this template use File | Settings | File Templates.
 */
public class GithubGetGistDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(GithubGetGistDialog.class);
    private final JComponent myComponent;
    private CollectionListModel<String> myFields;

    public GithubGetGistDialog(@Nullable Project project) {
        super(project, true);

        myFields = new CollectionListModel<String>("johna", "johna");
        JBList gistList = new JBList(myFields);
        gistList.setCellRenderer(new DefaultListCellRenderer());
//        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(gistList);
//        JPanel panel = decorator.createPanel();
        SimpleToolWindowPanel simpleToolWindowPanel = new SimpleToolWindowPanel(true);
        simpleToolWindowPanel.add(gistList);

        final Set<EditorCustomization> editorFeatures = ContainerUtil.newHashSet();
        editorFeatures.add(SpellCheckingEditorCustomization.ENABLED);

//        if (defaultLines == 1) {
//            editorFeatures.add(HorizontalScrollBarEditorCustomization.DISABLED);
//            editorFeatures.add(OneLineEditorCustomization.ENABLED);
//        } else {
        editorFeatures.add(SoftWrapsEditorCustomization.ENABLED);
//        }

        EditorTextFieldProvider service = ServiceManager.getService(project, EditorTextFieldProvider.class);
        simpleToolWindowPanel.add(service.getEditorField(FileTypes.PLAIN_TEXT.getLanguage(), project, editorFeatures));

        myComponent = simpleToolWindowPanel;//LabeledComponent.create(panel, "Get gist");
        setTitle("Get gist for github");

        final Factory<UsageSearcher> searcherFactory = new Factory<UsageSearcher>() {
            @Override
            public UsageSearcher create() {
                return new UsageSearcher() {
                    @Override
                    public void generate(final Processor<Usage> processor) {
//                        myIsFindInProgress = true;
//
//                        try {
//                            FindInProjectUtil.findUsages(findModelCopy, psiDirectory, myProject,
//                                    true, new AdapterProcessor<UsageInfo, Usage>(processor, UsageInfo2UsageAdapter.CONVERTER));
//                        }
//                        finally {
//                            myIsFindInProgress = false;
//                        }
                    }
                };
            }
        };

        showUsageView(project, searcherFactory);

        init();
    }


    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return myComponent;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getSourceCode() {
        return "johnathan";  //To change body of created methods use File | Settings | File Templates.
    }

    public static void showUsageView(final Project project, Factory<UsageSearcher> searcherFactory) {
        final UsageViewPresentation presentation = new UsageViewPresentation();
        presentation.setTargetsNodeText("Expression");
        presentation.setCodeUsages(false);
        presentation.setCodeUsagesString("Result");
        presentation.setNonCodeUsagesString("Result");
        presentation.setUsagesString("XPath Result");
        presentation.setUsagesWord("match");
        presentation.setTabText("XPath");
        presentation.setScopeText("XML Files");

//        presentation.setOpenInNewTab(XPathAppComponent.getInstance().getConfig().OPEN_NEW_TAB);

        final FindUsagesProcessPresentation processPresentation = new FindUsagesProcessPresentation();
        processPresentation.setProgressIndicatorFactory(new Factory<ProgressIndicator>() {
            @Override
            public ProgressIndicator create() {
                return new FindProgressIndicator(project, "XML Document(s)");
            }
        });
        processPresentation.setShowPanelIfOnlyOneUsage(true);
        processPresentation.setShowNotFoundMessage(true);
        final UsageTarget[] usageTargets = {};

        UsageViewManager.getInstance(project).searchAndShowUsages(
                usageTargets,
                searcherFactory,
                processPresentation,
                presentation,
                new UsageViewManager.UsageViewStateListener() {
                    @Override
                    public void usageViewCreated(@NotNull UsageView usageView) {
//                        usageView.addButtonToLowerPane(editAction, "&Edit Expression");
                    }

                    @Override
                    public void findingUsagesFinished(UsageView usageView) {
                    }
                });
    }

}
