/*
 * Copyright 2014 Lukas Krejci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package org.revapi.java;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.revapi.API;
import org.revapi.AnalysisContext;
import org.revapi.AnalysisResult;
import org.revapi.Report;
import org.revapi.Revapi;
import org.revapi.java.spi.Code;
import org.revapi.java.spi.JavaModelElement;

/**
 * @author Lukas Krejci
 * @since 0.1
 */
public class SupplementaryJarsTest extends AbstractJavaElementAnalyzerTest {

    private ArchiveAndCompilationPath compRes1;
    private ArchiveAndCompilationPath compRes2;

    private JavaArchive apiV1;
    private JavaArchive apiV2;
    private JavaArchive supV1;
    private JavaArchive supV2;

    @Before
    public void compile() throws Exception {
        //compile all the classes we need in 1 go
        compRes1 = createCompiledJar("tmp1", "v1/supplementary/a/A.java", "v1/supplementary/b/B.java",
                "v1/supplementary/a/C.java");

        //now, create 2 jars out of them. Class A will be our "api" jar and the rest of the classes will form the
        //supplementary jar that needs to be present as a runtime dep of the API but isn't itself considered an API of
        //of its own.
        //We then check that types from such supplementary jar that the API jar "leaks" by exposing them as types
        //in public/protected fields/methods/method params are then considered the part of the API during api checks.

        apiV1 = ShrinkWrap.create(JavaArchive.class, "apiV1.jar")
                .addAsResource(compRes1.compilationPath.resolve("A.class").toFile(), "A.class")
                .addAsResource(compRes1.compilationPath.resolve("C.class").toFile(), "C.class");

        supV1 = ShrinkWrap.create(JavaArchive.class, "supV1.jar")
                .addAsResource(compRes1.compilationPath.resolve("B.class").toFile(), "B.class")
                .addAsResource(compRes1.compilationPath.resolve("B$T$1.class").toFile(), "B$T$1.class")
                .addAsResource(compRes1.compilationPath.resolve("B$T$1$TT$1.class").toFile(), "B$T$1$TT$1.class")
                .addAsResource(compRes1.compilationPath.resolve("B$T$2.class").toFile(), "B$T$2.class")
                .addAsResource(compRes1.compilationPath.resolve("B$UsedByIgnoredClass.class").toFile(),
                        "B$UsedByIgnoredClass.class")
                .addAsResource(compRes1.compilationPath.resolve("A$PrivateEnum.class").toFile(), "A$PrivateEnum.class");

        //now do the same for v2
        compRes2 = createCompiledJar("tmp2", "v2/supplementary/a/A.java", "v2/supplementary/b/B.java",
                "v2/supplementary/a/C.java");

        apiV2 = ShrinkWrap.create(JavaArchive.class, "apiV2.jar")
                .addAsResource(compRes2.compilationPath.resolve("A.class").toFile(), "A.class")
                .addAsResource(compRes2.compilationPath.resolve("C.class").toFile(), "C.class");
        supV2 = ShrinkWrap.create(JavaArchive.class, "supV2.jar")
                .addAsResource(compRes2.compilationPath.resolve("B.class").toFile(), "B.class")
                .addAsResource(compRes2.compilationPath.resolve("B$T$1.class").toFile(), "B$T$1.class")
                .addAsResource(compRes2.compilationPath.resolve("B$T$1$TT$1.class").toFile(), "B$T$1$TT$1.class")
                .addAsResource(compRes2.compilationPath.resolve("B$T$2.class").toFile(), "B$T$2.class")
                .addAsResource(compRes2.compilationPath.resolve("B$T$1$Private.class").toFile(), "B$T$1$Private.class")
                .addAsResource(compRes2.compilationPath.resolve("B$T$3.class").toFile(), "B$T$3.class")
                .addAsResource(compRes2.compilationPath.resolve("B$PrivateSuperClass.class").toFile(),
                        "B$PrivateSuperClass.class")
                .addAsResource(compRes2.compilationPath.resolve("B$PrivateUsedClass.class").toFile(),
                        "B$PrivateUsedClass.class")
                .addAsResource(compRes2.compilationPath.resolve("B$UsedByIgnoredClass.class").toFile(),
                        "B$UsedByIgnoredClass.class")
                .addAsResource(compRes2.compilationPath.resolve("A$PrivateEnum.class").toFile(), "A$PrivateEnum.class")
                .addAsResource(compRes2.compilationPath.resolve("B$PrivateBase.class").toFile(), "B$PrivateBase.class");
    }

    @After
    public void delete() throws Exception {
        deleteDir(compRes1.compilationPath);
        deleteDir(compRes2.compilationPath);
    }

    @Test
    public void testSupplementaryJarsAreTakenIntoAccountWhenComputingAPI() throws Exception {
        List<Report> allReports;

        AnalysisContext ctx = AnalysisContext.builder()
                .withOldAPI(API.of(new ShrinkwrapArchive(apiV1)).supportedBy(new ShrinkwrapArchive(supV1)).build())
                .withNewAPI(API.of(new ShrinkwrapArchive(apiV2)).supportedBy(new ShrinkwrapArchive(supV2)).build())
                .build();
        Revapi revapi = createRevapi(CollectingReporter.class);

        try (AnalysisResult res = revapi.analyze(ctx)) {
            Assert.assertTrue(res.isSuccess());
            allReports = res.getExtensions().getFirstExtension(CollectingReporter.class, null).getReports();
        }

        Assert.assertEquals(8 + 11, allReports.size()); //11 removed methods when kind of class changes to interface
        Assert.assertTrue(
                containsDifference(allReports, null, "class B.T$1.Private", Code.CLASS_NON_PUBLIC_PART_OF_API.code()));
        Assert.assertTrue(containsDifference(allReports, null, "field B.T$2.f2", Code.FIELD_ADDED.code()));
        Assert.assertTrue(containsDifference(allReports, null, "field A.f3", Code.FIELD_ADDED.code()));
        Assert.assertTrue(containsDifference(allReports, "class B.T$2", "class B.T$2", Code.CLASS_NOW_FINAL.code()));
        Assert.assertTrue(containsDifference(allReports, null, "class B.T$3", Code.CLASS_ADDED.code()));
        Assert.assertTrue(containsDifference(allReports, null, "class B.PrivateUsedClass",
                Code.CLASS_NON_PUBLIC_PART_OF_API.code()));
        Assert.assertTrue(containsDifference(allReports, "class B.UsedByIgnoredClass", "interface B.UsedByIgnoredClass",
                Code.CLASS_KIND_CHANGED.code()));
        Assert.assertTrue(containsDifference(allReports, "method void B.UsedByIgnoredClass::<init>()", null,
                Code.METHOD_REMOVED.code()));
        //eleven methods removed when kind changed, because interface doesn't have the methods of Object
        Assert.assertEquals(11, allReports.stream()
                .filter(r -> {
                    javax.lang.model.element.TypeElement oldType = null;
                    if (r.getOldElement() == null || !(r.getOldElement() instanceof JavaModelElement)) {
                        return false;
                    }

                    javax.lang.model.element.Element old = ((JavaModelElement) r.getOldElement()).getDeclaringElement();

                    do {
                        if (old instanceof javax.lang.model.element.TypeElement) {
                            oldType = (javax.lang.model.element.TypeElement) old;
                            break;
                        }
                        old = old.getEnclosingElement();
                    } while (old != null);

                    if (oldType == null) {
                        return false;
                    }

                    return oldType.getQualifiedName().contentEquals("java.lang.Object");

                })
                .flatMap(r -> r.getDifferences().stream())
                .count());
    }

    @Test
    public void testExcludedClassesDontDragUsedTypesIntoAPI() throws Exception {
        List<Report> allReports;
        AnalysisContext ctx = AnalysisContext.builder()
                .withOldAPI(API.of(new ShrinkwrapArchive(apiV1)).supportedBy(new ShrinkwrapArchive(supV1)).build())
                .withNewAPI(API.of(new ShrinkwrapArchive(apiV2)).supportedBy(new ShrinkwrapArchive(supV2)).build())
                .withConfigurationFromJSON("{\"revapi\": {\"java\": {" +
                        "\"filter\": {\"classes\": {\"exclude\": [\"C\"]}}}}}").build();

        Revapi revapi = createRevapi(CollectingReporter.class);

        try (AnalysisResult res = revapi.analyze(ctx)) {
            allReports =
                    res.getExtensions().getFirstExtension(CollectingReporter.class, null).getReports();
        }

        Assert.assertEquals(6, allReports.size());
        Assert.assertTrue(
                containsDifference(allReports, null, "class B.T$1.Private", Code.CLASS_NON_PUBLIC_PART_OF_API.code()));
        Assert.assertTrue(containsDifference(allReports, null, "field B.T$2.f2", Code.FIELD_ADDED.code()));
        Assert.assertTrue(containsDifference(allReports, null, "field A.f3", Code.FIELD_ADDED.code()));
        Assert.assertTrue(containsDifference(allReports, "class B.T$2", "class B.T$2", Code.CLASS_NOW_FINAL.code()));
        Assert.assertTrue(containsDifference(allReports, null, "class B.T$3", Code.CLASS_ADDED.code()));
        Assert.assertTrue(containsDifference(allReports, null, "class B.PrivateUsedClass",
                Code.CLASS_NON_PUBLIC_PART_OF_API.code()));
        Assert.assertFalse(containsDifference(allReports, "class B.UsedByIgnoredClass", "class B.UsedByIgnoredClass",
                Code.CLASS_KIND_CHANGED.code()));
        Assert.assertFalse(containsDifference(allReports, "method void B.UsedByIgnoredClass::<init>()", null,
                Code.METHOD_REMOVED.code()));
    }

    @Test
    public void testExcludedClassesInAPI() throws Exception {
        List<Report> allReports;
        AnalysisContext ctx = AnalysisContext.builder()
                .withOldAPI(API.of(new ShrinkwrapArchive(apiV1)).supportedBy(new ShrinkwrapArchive(supV1)).build())
                .withNewAPI(API.of(new ShrinkwrapArchive(apiV2)).supportedBy(new ShrinkwrapArchive(supV2)).build())
                .withConfigurationFromJSON("{\"revapi\": {\"java\": {" +
                        "\"filter\": {\"classes\": {\"exclude\": [\"C\", \"B.T$2\"]}}}}}").build();

        Revapi revapi = createRevapi(CollectingReporter.class);

        try (AnalysisResult res = revapi.analyze(ctx)) {
            allReports =
                    res.getExtensions().getFirstExtension(CollectingReporter.class, null).getReports();
        }

        Assert.assertEquals(3, allReports.size());
        Assert.assertFalse(
                containsDifference(allReports, null, "class B.T$1.Private", Code.CLASS_NON_PUBLIC_PART_OF_API.code()));
        Assert.assertFalse(containsDifference(allReports, null, "field B.T$2.f2", Code.FIELD_ADDED.code()));
        Assert.assertTrue(containsDifference(allReports, null, "field A.f3", Code.FIELD_ADDED.code()));
        Assert.assertFalse(containsDifference(allReports, "class B.T$2", "class B.T$2", Code.CLASS_NOW_FINAL.code()));
        Assert.assertTrue(containsDifference(allReports, null, "class B.T$3", Code.CLASS_ADDED.code()));
        Assert.assertTrue(containsDifference(allReports, null, "class B.PrivateUsedClass",
                Code.CLASS_NON_PUBLIC_PART_OF_API.code()));
        Assert.assertFalse(containsDifference(allReports, "class B.UsedByIgnoredClass", "class B.UsedByIgnoredClass",
                Code.CLASS_KIND_CHANGED.code()));
        Assert.assertFalse(containsDifference(allReports, "method void B.UsedByIgnoredClass::<init>()", null,
                Code.METHOD_REMOVED.code()));
    }
}
