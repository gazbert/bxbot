/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 gazbert
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.gazbert.bxbot.rest.api.jbehave;

import static org.jbehave.core.reporters.Format.CONSOLE;
import static org.jbehave.core.reporters.Format.HTML;
import static org.jbehave.core.reporters.Format.TXT;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.jbehave.core.Embeddable;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.embedder.StoryControls;
import org.jbehave.core.io.CodeLocations;
import org.jbehave.core.io.LoadFromClasspath;
import org.jbehave.core.io.UnderscoredCamelCaseResolver;
import org.jbehave.core.junit.JUnitStories;
import org.jbehave.core.reporters.FilePrintStreamFactory.ResolveToPackagedName;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.InstanceStepsFactory;
import org.jbehave.core.steps.ParameterConverters;
import org.jbehave.core.steps.ParameterConverters.DateConverter;

/**
 * Base class for all JBehave BDD tests.
 *
 * @author gazbert
 */
public abstract class AbstractStory extends JUnitStories {

  public abstract String storyName();

  public abstract Object stepInstance();

  @Override
  public Configuration configuration() {

    final Class<? extends Embeddable> embeddableClass = this.getClass();
    final Properties viewResources = new Properties();
    viewResources.put("decorateNonHtml", "true");

    final ParameterConverters parameterConverters = new ParameterConverters();
    parameterConverters.addConverters(new DateConverter(new SimpleDateFormat("yyyy-MM-dd")));

    return new MostUsefulConfiguration()
        .useStoryControls(new StoryControls().doDryRun(false).doSkipScenariosAfterFailure(false))
        .useStoryLoader(new LoadFromClasspath(embeddableClass))
        .useStoryPathResolver(new UnderscoredCamelCaseResolver())
        .useStoryReporterBuilder(
            new StoryReporterBuilder()
                .withCodeLocation(CodeLocations.codeLocationFromClass(embeddableClass))
                .withDefaultFormats()
                .withPathResolver(new ResolveToPackagedName())
                .withViewResources(viewResources)
                .withFormats(CONSOLE, TXT, HTML)
                .withFailureTrace(true)
                .withFailureTraceCompression(true))
        .useParameterConverters(parameterConverters);
  }

  @Override
  public InjectableStepsFactory stepsFactory() {
    return new InstanceStepsFactory(configuration(), stepInstance());
  }

  @Override
  protected List<String> storyPaths() {
    return Collections.singletonList(storyName());
  }
}
