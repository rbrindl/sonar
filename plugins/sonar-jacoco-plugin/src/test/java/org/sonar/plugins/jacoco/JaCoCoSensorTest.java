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
package org.sonar.plugins.jacoco;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.*;
import org.sonar.api.test.IsMeasure;

import java.io.File;

/**
 * @author Evgeny Mandrikov
 */
public class JaCoCoSensorTest {

  private JacocoConfiguration configuration;
  private JaCoCoSensor sensor;

  @Before
  public void setUp() {
    configuration = mock(JacocoConfiguration.class);
    sensor = new JaCoCoSensor(configuration);
  }

  @Test
  public void testSensorDefinition() {
    assertThat(sensor.toString(), is("JaCoCoSensor"));
  }

  @Test
  public void shouldNotExecuteOnProject() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn("flex");
    assertThat(sensor.shouldExecuteOnProject(project), is(false));
  }

  @Test
  public void shouldExecuteOnProject() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn(Java.KEY);
    assertThat(sensor.shouldExecuteOnProject(project), is(true));
  }

  @Test
  public void testReadExecutionData() throws Exception {
    File jacocoExecutionData = new File(getClass().getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest/jacoco.exec").getFile());
    File buildOutputDir = jacocoExecutionData.getParentFile();
    SensorContext context = mock(SensorContext.class);

    final JavaFile resource = new JavaFile("org.sonar.plugins.jacoco.tests.Hello");
    when(context.getResource(any(Resource.class))).thenReturn(resource);

    ProjectFileSystem pfs = mock(ProjectFileSystem.class);
    when(pfs.getBuildOutputDir()).thenReturn(buildOutputDir);
    when(pfs.resolvePath(anyString())).thenReturn(jacocoExecutionData);

    Project project = mock(Project.class);
    when(project.getFileSystem()).thenReturn(pfs);

    sensor.analyse(project, context);

    verify(context).getResource(eq(resource));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER, 7.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES, 3.0)));
    verify(context).saveMeasure(eq(resource),
        argThat(new IsMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA, "6=1;7=1;8=1;11=1;15=0;16=0;18=0")));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER, 2.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS, 2.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.CONDITIONS_BY_LINE, "15=2")));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.COVERED_CONDITIONS_BY_LINE, "15=0")));
    verifyNoMoreInteractions(context);
  }

  @Test
  public void doNotSaveMeasureOnResourceWhichDoesntExistInTheContext() throws Exception {
    File jacocoExecutionData = new File(getClass().getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest/jacoco.exec").getFile());
    File buildOutputDir = jacocoExecutionData.getParentFile();
    SensorContext context = mock(SensorContext.class);
    when(context.getResource(any(Resource.class))).thenReturn(null);

    ProjectFileSystem pfs = mock(ProjectFileSystem.class);
    when(pfs.getBuildOutputDir()).thenReturn(buildOutputDir);

    Project project = mock(Project.class);
    when(project.getFileSystem()).thenReturn(pfs);

    sensor.analyse(project, context);

    verify(context, never()).saveMeasure(any(Resource.class), any(Measure.class));
  }
}
