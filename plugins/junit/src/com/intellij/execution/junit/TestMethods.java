/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.execution.junit;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.Location;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.execution.junit2.info.MethodLocation;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.testframework.AbstractTestProxy;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class TestMethods extends TestMethod {
  private static final Logger LOG = Logger.getInstance(TestMethods.class);

  private final Collection<AbstractTestProxy> myFailedTests;

  public TestMethods(@NotNull JUnitConfiguration configuration,
                     @NotNull ExecutionEnvironment environment,
                     @NotNull Collection<AbstractTestProxy> failedTests) {
    super(configuration, environment);

    myFailedTests = failedTests;
  }

  @Override
  protected JavaParameters createJavaParameters() throws ExecutionException {
    final JavaParameters javaParameters = super.createDefaultJavaParameters();
    final JUnitConfiguration.Data data = getConfiguration().getPersistentData();
    RunConfigurationModule module = getConfiguration().getConfigurationModule();
    final Project project = module.getProject();
    final GlobalSearchScope searchScope = getConfiguration().getConfigurationModule().getSearchScope();
    addClassesListToJavaParameters(myFailedTests, new Function<AbstractTestProxy, String>() {
      @Override
      public String fun(AbstractTestProxy testInfo) {
        return testInfo != null ? getTestPresentation(testInfo, project, searchScope) : null;
      }
    }, data.getPackageName(), true, javaParameters);

    return javaParameters;
  }

  @Nullable
  public static String getTestPresentation(AbstractTestProxy testInfo, Project project, GlobalSearchScope searchScope) {
    final Location location = testInfo.getLocation(project, searchScope);
    final PsiElement element = location != null ? location.getPsiElement() : null;
    if (element instanceof PsiMethod) {
      final PsiClass containingClass = location instanceof MethodLocation ? ((MethodLocation)location).getContainingClass() 
                                                                          : ((PsiMethod)element).getContainingClass();
      if (containingClass != null) {
        final String proxyName = testInfo.getName();
        final String methodName = ((PsiMethod)element).getName();
        return JavaExecutionUtil.getRuntimeQualifiedName(containingClass) + "," + proxyName.substring(proxyName.indexOf(methodName));
      }
    }
    return null;
  }

  @Override
  public String suggestActionName() {
    return ActionsBundle.message("action.RerunFailedTests.text");
  }
}
