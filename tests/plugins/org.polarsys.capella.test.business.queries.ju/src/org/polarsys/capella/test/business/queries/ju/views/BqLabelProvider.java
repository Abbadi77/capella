package org.polarsys.capella.test.business.queries.ju.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.graphics.Image;
import org.polarsys.capella.common.ui.providers.MDEAdapterFactoryLabelProvider;
import org.polarsys.capella.test.business.queries.ju.QueryResult;
import org.polarsys.capella.test.business.queries.ju.ResultItem;
import org.polarsys.capella.test.business.queries.ju.ResultItem.Kind;
import org.polarsys.capella.test.business.queries.ju.TestBusinessQueriesPlugin;
import org.polarsys.kitalpha.emde.model.Element;

public class BqLabelProvider extends MDEAdapterFactoryLabelProvider {

  public BqLabelProvider() {
    super();
  }

  @Override
  public String getText(Object object) {

    if (object instanceof QueryResult) {
      String prefix = ((QueryResult) object).getBusinessQuery() == null ? "NO-QUERY ": "";
      
      EObject adapt = Adapters.adapt(object, Element.class);
      if (adapt != null) {
        return prefix+ super.getText(adapt) + " " + ((QueryResult) object).getQueryIdentifier();
      }
      return object.toString();
    } else if (object instanceof ResultItem) {
      EObject adapt = Adapters.adapt(object, Element.class);
      if (adapt != null) {
        Kind kind = ((ResultItem)object).getKind();
        if (kind == Kind.MISSING) {
          return "Expected : " +super.getText(adapt);
        }
        if (kind == Kind.ADDED) {
          return "Unexpected : " +super.getText(adapt);
        }
        return ((ResultItem)object).kind + " " +super.getText(adapt);
      }
      return object.toString();
    }
    return object.toString();
  }

  @Override
  public Image getImage(Object object) {
    if (object instanceof IFile) {
      return ImageExt2.getImage(FrameworkUtil.getBundle(TestBusinessQueriesPlugin.class).getSymbolicName(), "full/obj16/test.png");
    }
    if (object instanceof QueryResult) {
      if (((QueryResult) object).getBusinessQuery() == null) {
        return ImageExt2.getImage(FrameworkUtil.getBundle(TestBusinessQueriesPlugin.class).getSymbolicName(), "full/obj16/error_tsk.png");
      }
      EObject adapt = Adapters.adapt(object, Element.class);
      if (adapt != null) {
        return super.getImage(adapt);
      }
    } else if (object instanceof ResultItem) {
      if (((ResultItem)object).getKind() == Kind.ADDED) {
        return ImageExt2.getImage(FrameworkUtil.getBundle(TestBusinessQueriesPlugin.class).getSymbolicName(), "full/obj16/add_obj.png");
      }
      if (((ResultItem)object).getKind() == Kind.MISSING) {
        return ImageExt2.getImage(FrameworkUtil.getBundle(TestBusinessQueriesPlugin.class).getSymbolicName(), "full/obj16/delete_edit.png");
      }
      EObject adapt = Adapters.adapt(object, Element.class);
      if (adapt != null) {
        return super.getImage(adapt);
      }
    }
    return super.getImage(object);
  }
  
}
