/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.cobertura;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.*;
import org.sonar.api.test.IsMeasure;
import org.sonar.api.test.IsResource;

import java.io.File;
import java.net.URISyntaxException;

public class CoberturaSensorTest {

  private SensorContext context;
  private CoberturaSensor sensor;

  @Before
  public void setUp() {
    context = mock(SensorContext.class);
    sensor = new CoberturaSensor();
  }

  @Test
  public void shouldNotFailIfReportNotSpecifiedOrNotFound() throws URISyntaxException {
    ProjectFileSystem pfs = mock(ProjectFileSystem.class);
    when(pfs.resolvePath(anyString()))
        .thenReturn(new File("notFound.xml"));

    Project project = mock(Project.class);
    when(project.getFileSystem()).thenReturn(pfs);
    when(project.getProperty(eq(CoreProperties.COBERTURA_REPORT_PATH_PROPERTY)))
        .thenReturn("notFound.xml")
        .thenReturn(null);

    sensor.analyse(project, context);
    sensor.analyse(project, context);
  }

  @Test
  public void doNotCollectProjectCoverage() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.COVERAGE), anyDouble());
  }

  @Test
  public void doNotCollectProjectLineCoverage() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.LINE_COVERAGE), anyDouble());
    verify(context, never()).saveMeasure(argThat(new IsMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA)));
  }

  @Test
  public void doNotCollectProjectBranchCoverage() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.BRANCH_COVERAGE), anyDouble());
  }

  @Test
  public void collectPackageLineCoverage() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    verify(context, never()).saveMeasure((Resource) argThat(is(JavaPackage.class)), eq(CoreMetrics.LINE_COVERAGE), anyDouble());
    verify(context, never()).saveMeasure((Resource) argThat(is(JavaPackage.class)), eq(CoreMetrics.UNCOVERED_LINES), anyDouble());
  }

  @Test
  public void collectPackageBranchCoverage() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    verify(context, never()).saveMeasure((Resource) argThat(is(JavaPackage.class)), eq(CoreMetrics.BRANCH_COVERAGE), anyDouble());
    verify(context, never()).saveMeasure((Resource) argThat(is(JavaPackage.class)), eq(CoreMetrics.UNCOVERED_CONDITIONS), anyDouble());
  }

  @Test
  public void packageCoverageIsCalculatedLaterByDecorator() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    verify(context, never()).saveMeasure((Resource) argThat(is(JavaPackage.class)), eq(CoreMetrics.COVERAGE), anyDouble());
  }

  @Test
  public void collectFileLineCoverage() throws URISyntaxException {
    when(context.getResource(any(Resource.class))).thenReturn(new JavaFile("org.sonar.MyClass"));
    sensor.parseReport(getCoverageReport(), context);

    final JavaFile file = new JavaFile("org.apache.commons.chain.config.ConfigParser");
    verify(context).saveMeasure(eq(file), argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER, 30.0)));
    verify(context).saveMeasure(eq(file), argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES, 5.0)));
  }

  @Test
  public void collectFileBranchCoverage() throws URISyntaxException {
    when(context.getResource(any(Resource.class))).thenReturn(new JavaFile("org.sonar.MyClass"));
    sensor.parseReport(getCoverageReport(), context);

    final JavaFile file = new JavaFile("org.apache.commons.chain.config.ConfigParser");
    verify(context).saveMeasure(eq(file), argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER, 6.0)));
    verify(context).saveMeasure(eq(file), argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS, 2.0)));
  }

  @Test
  public void testDoNotSaveMeasureOnResourceWhichDoesntExistInTheContext() throws URISyntaxException {
    when(context.getResource(any(Resource.class))).thenReturn(null);
    sensor.parseReport(getCoverageReport(), context);
    verify(context, never()).saveMeasure(any(Resource.class), any(Measure.class));
  }

  @Test
  public void javaInterfaceHasNoCoverage() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    final JavaFile interfaze = new JavaFile("org.apache.commons.chain.Chain");
    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.COVERAGE)));

    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.LINE_COVERAGE)));
    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER)));
    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES)));

    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.BRANCH_COVERAGE)));
    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER)));
    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS)));
  }

  @Test
  public void shouldInsertCoverageAtFileLevel() throws URISyntaxException {
    File coverage = new File(getClass().getResource(
        "/org/sonar/plugins/cobertura/CoberturaSensorTest/shouldInsertCoverageAtFileLevel/coverage.xml").toURI());
    when(context.getResource(any(Resource.class))).thenReturn(new JavaFile("org.sonar.MyClass"));
    sensor.parseReport(coverage, context);

    verify(context).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.InnerClass")),
        argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER, 35.0)));
    verify(context).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.InnerClass")),
        argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES, 22.0)));

    verify(context).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.InnerClass")),
        argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER, 4.0)));
    verify(context).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.InnerClass")),
        argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS, 3.0)));

    verify(context, never()).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.InnerClass$InnerClassInside")),
        argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.InnerClass$InnerClassInside")),
        argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.InnerClass$InnerClassInside")),
        argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.InnerClass$InnerClassInside")),
        argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES)));

    verify(context, never()).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.PrivateClass")),
        argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.PrivateClass")),
        argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.PrivateClass")),
        argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.PrivateClass")),
        argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES)));

    verify(context)
        .saveMeasure(
            eq(new JavaFile("org.sonar.samples.InnerClass")),
            argThat(new IsMeasure(
                CoreMetrics.COVERAGE_LINE_HITS_DATA,
                "22=2;25=0;26=0;29=0;30=0;31=0;34=1;35=1;36=1;37=0;39=1;41=1;44=2;46=1;47=1;50=0;51=0;52=0;53=0;55=0;57=0;60=0;61=0;64=1;71=1;73=1;76=0;77=0;80=0;81=0;85=0;87=0;91=0;93=0;96=1")));
  }

  @Test
  public void collectFileLineHitsData() throws URISyntaxException {
    when(context.getResource(any(Resource.class))).thenReturn(new JavaFile("org.sonar.MyClass"));
    sensor.parseReport(getCoverageReport(), context);
    verify(context).saveMeasure(
        eq(new JavaFile("org.apache.commons.chain.impl.CatalogBase")),
        argThat(new IsMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA,
            "48=117;56=234;66=0;67=0;68=0;84=999;86=999;98=318;111=18;121=0;122=0;125=0;126=0;127=0;128=0;131=0;133=0")));
  }

  @Test
  public void shouldNotCountTwiceAnonymousClasses() throws URISyntaxException {
    File coverage = new File(getClass().getResource("/org/sonar/plugins/cobertura/CoberturaSensorTest/shouldNotCountTwiceAnonymousClasses.xml").toURI());
    when(context.getResource(any(Resource.class))).thenReturn(new JavaFile("org.sonar.samples.MyClass"));
    sensor.parseReport(coverage, context);

    verify(context).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.MyFile")),
        argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER, 5.0))); // do not count line 26 twice
  }

  private File getCoverageReport() throws URISyntaxException {
    return new File(getClass().getResource("/org/sonar/plugins/cobertura/CoberturaSensorTest/commons-chain-coverage.xml").toURI());
  }
}
