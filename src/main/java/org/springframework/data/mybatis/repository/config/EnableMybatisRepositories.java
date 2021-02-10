package org.springframework.data.mybatis.repository.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.data.mybatis.repository.support.MybatisRepositoryFactoryBean;

/**
 * Annotation to enable Mybatis repositories. Will scan the package of the annotated configuration class for Spring Data
 * repositories by default.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(MybatisRepositoriesRegistrar.class)
public @interface EnableMybatisRepositories {

    /**
     * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation declarations e.g.:
     * {@code @EnableMybatisRepositories("org.my.pkg")} instead of
     * {@code @EnableMybatisRepositories(basePackages="org.my.pkg")}.
     * 
     * @return basePackages of repositories
     */
    String[] value() default {};

    /**
     * Base packages to scan for annotated components. {@link #value()} is an alias for (and mutually exclusive with) this
     * attribute. Use {@link #basePackageClasses()} for a type-safe alternative to String-based package names.
     * 
     * @return basePackages of repositories
     */
    String[] basePackages() default {};

    /**
     * Type-safe alternative to {@link #basePackages()} for specifying the packages to scan for annotated components. The
     * package of each class specified will be scanned. Consider creating a special no-op marker class or interface in
     * each package that serves no purpose other than being referenced by this attribute.
     * 
     * @return basePackageClasses of repositories
     */
    Class<?>[] basePackageClasses() default {};

    /**
     * Specifies which types are eligible for component scanning. Further narrows the set of candidate components from
     * everything in {@link #basePackages()} to everything in the base packages that matches the given filter or filters.
     * 
     * @return includeFilters
     */
    Filter[] includeFilters() default {};

    /**
     * Specifies which types are not eligible for component scanning.
     * 
     * @return excludeFilters
     */
    Filter[] excludeFilters() default {};

    /**
     * Configures whether nested repository-interfaces (e.g. defined as inner classes) should be discovered by the
     * repositories infrastructure.
     * 
     * @return considerNestedRepositories
     */
    boolean considerNestedRepositories() default false;

    /**
     * Returns the {@link FactoryBean} class to be used for each repository instance. Defaults to
     * {@link MybatisRepositoryFactoryBean}.
     * 
     * @return repositoryFactoryBeanClass
     */
    Class<?> repositoryFactoryBeanClass() default MybatisRepositoryFactoryBean.class;

    /**
     * Configures the location of where to find the Spring Data named queries properties file. Will default to
     * {@code META-INF/mybatis-named-queries.properties}.
     * 
     * @return namedQueriesLocation
     */
    String namedQueriesLocation() default "";
    
    /**
     * Returns the postfix to be used when looking up custom repository implementations. Defaults to {@literal Impl}. So
     * for a repository named {@code PersonRepository} the corresponding implementation class will be looked up scanning
     * for {@code PersonRepositoryImpl}.
     * 
     * @return repositoryImplementationPostfix
     */
    String repositoryImplementationPostfix() default "Impl";
    
}
