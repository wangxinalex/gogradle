/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.github.blindpirate.gogradle.core.dependency.produce.strategy

import com.github.blindpirate.gogradle.GogradleRunner
import com.github.blindpirate.gogradle.core.dependency.VendorResolvedDependencyForTest
import org.junit.Test
import org.junit.runner.RunWith

import static com.github.blindpirate.gogradle.core.dependency.AbstractResolvedDependencyTest.ResolvedDependencyForTest
import static org.mockito.Mockito.*

@RunWith(GogradleRunner)
class DefaultDependencyProduceStrategyTest extends DependencyProduceStrategyTest {
    DefaultDependencyProduceStrategy strategy = new DefaultDependencyProduceStrategy()

    @Test
    void 'source code will be scanned when external and vendor dependencies are all empty'() {
        // given
        externalDependencies()
        vendorDependencies()
        sourceCodeDependencies()

        // when
        assert strategy.produce(resolvedDependency, rootDir, visitor, 'build').isEmpty()

        //then
        verify(visitor).visitSourceCodeDependencies(resolvedDependency, rootDir, 'build')
    }

    @Test
    void 'source code will be scanned when subpackage is specified'() {
        // given
        vendorDependencies(a1, b1)
        externalDependencies(a2, c2)
        sourceCodeDependencies()
        when(resolvedDependency.getSubpackages()).thenReturn(['sub'] as Set)

        // when
        assert strategy.produce(resolvedDependency, rootDir, visitor, 'build').isEmpty()

        //then
        verify(visitor).visitSourceCodeDependencies(resolvedDependency, rootDir, 'build')
    }

    @Test
    void 'vendor dependencies should have priority over external dependencies'() {
        // given
        vendorDependencies(a1, b1)
        externalDependencies(a2, c2)

        // when
        def result = strategy.produce(resolvedDependency, rootDir, visitor, 'build')
        // then
        assert !result.any { it.is(a2) }
        assert result.any { it.is(b1) }
        assert result.any { it.is(c2) }
        assert result.any { it.is(a1) }
        verify(visitor, times(0)).visitSourceCodeDependencies(resolvedDependency, rootDir, 'build')
    }

    @Test
    void 'cascading vendor dependencies should be handled correctly'() {
        // given
        ResolvedDependencyForTest a = new ResolvedDependencyForTest('a', 'a', 1L, null)

        VendorResolvedDependencyForTest b = new VendorResolvedDependencyForTest('b', 'b', 1L, a, 'vendor/b')
        VendorResolvedDependencyForTest c = new VendorResolvedDependencyForTest('c', 'c', 1L, a, 'vendor/b/vendor/c')

        a.dependencies.add(b)
        b.dependencies.add(c)

        vendorDependencies(a)
        externalDependencies(b2)

        // when
        def result = strategy.produce(resolvedDependency, rootDir, visitor, 'build')
        // then
        assert result.any { it.is(a) }
        assert !result.any { it.is(b2) }
        verify(visitor, times(0)).visitSourceCodeDependencies(resolvedDependency, rootDir, 'build')
    }

    @Test
    void 'vendor dependencies should be used when external dependencies are empty'() {
        // given
        vendorDependencies(a1)
        externalDependencies()
        // when
        def result = strategy.produce(resolvedDependency, rootDir, visitor, 'build')
        // then
        assert result.size() == 1
        assert result.any { it.is(a1) }
        verify(visitor, times(0)).visitSourceCodeDependencies(resolvedDependency, rootDir, 'build')
    }

    @Test
    void 'external dependencies should be used when vendor are empty'() {
        // given
        vendorDependencies()
        externalDependencies(a2)
        // when
        def result = strategy.produce(resolvedDependency, rootDir, visitor, 'build')
        // then
        assert result.size() == 1
        assert result.any { it.is(a2) }
        verify(visitor, times(0)).visitSourceCodeDependencies(resolvedDependency, rootDir, 'build')
    }

}
