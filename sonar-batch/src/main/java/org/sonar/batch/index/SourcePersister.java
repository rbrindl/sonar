/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.batch.index;

import com.google.common.collect.Sets;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.database.model.SnapshotSource;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;

import java.util.Set;

public final class SourcePersister {

  private DatabaseSession session;
  private Set<Integer> savedSnapshotIds = Sets.newHashSet();
  private ResourcePersister resourcePersister;

  public SourcePersister(DatabaseSession session, ResourcePersister resourcePersister) {
    this.session = session;
    this.resourcePersister = resourcePersister;
  }

  public void saveSource(Project project, Resource resource, String source) {
    Snapshot snapshot = resourcePersister.saveResource(project, resource);
    if (snapshot != null) {

      if (savedSnapshotIds.contains(snapshot.getId())) {
        throw new SonarException("Can not set twice the source of " + resource);
      }
      session.save(new SnapshotSource(snapshot.getId(), source));
      session.commit();
      savedSnapshotIds.add(snapshot.getId());
    }
  }

  public void clear() {
    savedSnapshotIds.clear();
  }
}