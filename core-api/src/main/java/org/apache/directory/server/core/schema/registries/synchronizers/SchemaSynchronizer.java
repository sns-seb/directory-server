/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.schema.registries.synchronizers;


import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.commons.lang.NotImplementedException;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.entry.ServerAttribute;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerEntryUtils;
import org.apache.directory.server.core.schema.PartitionSchemaLoader;
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.schema.registries.SchemaObjectRegistry;
import org.apache.directory.shared.schema.loader.ldif.SchemaEntityFactory;


/**
 * @TODO poorly implemented - revisit the SchemaChangeHandler for this puppy
 * and do it right.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SchemaSynchronizer implements RegistrySynchronizer
{
    private final SchemaEntityFactory factory;
    private final PartitionSchemaLoader loader;
    
    /** The global registries */
    private final Registries registries;
    
    /** The m-disable AttributeType */
    private final AttributeType disabledAT;
    
    /** The CN attributeType */
    private final AttributeType cnAT;
    
    /** The m-dependencies AttributeType */
    private final AttributeType dependenciesAT;
    
    /** A static DN referencing ou=schema */
    private final LdapDN ouSchemaDN;


    /**
     * Creates and initializes a new instance of Schema synchronizer
     *
     * @param registries The Registries
     * @param loader The schema loader
     * @throws Exception If something went wrong
     */
    public SchemaSynchronizer( Registries registries, PartitionSchemaLoader loader ) throws Exception
    {
        this.registries = registries;
        disabledAT = registries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_DISABLED_AT );
        this.loader = loader;
        factory = new SchemaEntityFactory();
        cnAT = registries.getAttributeTypeRegistry().lookup( SchemaConstants.CN_AT );
        dependenciesAT = registries.getAttributeTypeRegistry()
            .lookup( MetaSchemaConstants.M_DEPENDENCIES_AT );
        
        ouSchemaDN = new LdapDN( ServerDNConstants.OU_SCHEMA_DN );
        ouSchemaDN.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
    }


    /**
     * Reacts to modification of a metaSchema object.  At this point the 
     * only considerable changes are to the m-disabled and the 
     * m-dependencies attributes.
     * 
     * @param name the dn of the metaSchema object modified
     * @param modOp the type of modification operation being performed
     * @param mods the attribute modifications as an Attributes object
     * @param entry the entry after the modifications have been applied
     */
    public boolean modify( LdapDN name, ModificationOperation modOp, ServerEntry mods, ServerEntry entry, 
        ServerEntry targetEntry, boolean cascade ) throws Exception
    {
        boolean hasModification = SCHEMA_UNCHANGED;

        EntryAttribute disabledInMods = mods.get( disabledAT );
        
        if ( disabledInMods != null )
        {
            disable( name, modOp, disabledInMods, entry.get( disabledAT ) );
        }
        
        // check if the new schema is enabled or disabled
        boolean isEnabled = false;
        EntryAttribute disabled = targetEntry.get( this.disabledAT );
        
        if ( disabled == null )
        {
            isEnabled = true;
        }
        else if ( ! disabled.getString().equals( "TRUE" ) )
        {
            isEnabled = true;
        }

        EntryAttribute dependencies = mods.get( dependenciesAT );
        
        if ( dependencies != null )
        {
            checkForDependencies( isEnabled, targetEntry );
        }
        
        return hasModification;
    }


    /**
     * Reacts to modification of a metaSchema object.  At this point the 
     * only considerable changes are to the m-disabled and the 
     * m-dependencies attributes.
     * 
     * @param name the dn of the metaSchema object modified
     * @param mods the attribute modifications as an ModificationItem arry
     * @param entry the entry after the modifications have been applied
     */
    public boolean modify( LdapDN name, List<Modification> mods, ServerEntry entry,
        ServerEntry targetEntry, boolean cascade ) throws Exception
    {
        boolean hasModification = SCHEMA_UNCHANGED;
        
        // Check if the entry has a m-disabled attribute 
        EntryAttribute disabledInEntry = entry.get( disabledAT );
        Modification disabledModification = ServerEntryUtils.getModificationItem( mods, disabledAT );
        
        if ( disabledModification != null )
        {
            // We are trying to modify the m-disabled attribute. 
            hasModification = disable( name, 
                     disabledModification.getOperation(), 
                     (ServerAttribute)disabledModification.getAttribute(), 
                     disabledInEntry );
        }

        // check if the new schema is enabled or disabled
        boolean isEnabled = false;
        EntryAttribute disabled = targetEntry.get( disabledAT );
        
        if ( disabled == null )
        {
            isEnabled = true;
        }
        else if ( ! disabled.contains( "TRUE" ) )
        {
            isEnabled = true;
        }

        ServerAttribute dependencies = ServerEntryUtils.getAttribute( mods, dependenciesAT );
        
        if ( dependencies != null )
        {
            checkForDependencies( isEnabled, targetEntry );
        }
        
        return hasModification;
    }


    public void moveAndRename( LdapDN oriChildName, LdapDN newParentName, Rdn newRn, boolean deleteOldRn, ServerEntry entry, boolean cascaded ) throws NamingException
    {

    }


    /**
     * Handles the addition of a metaSchema object to the schema partition.
     * 
     * @param name the dn of the new metaSchema object
     * @param entry the attributes of the new metaSchema object
     */
    public void add( LdapDN name, ServerEntry entry ) throws Exception
    {
        LdapDN parentDn = ( LdapDN ) name.clone();
        parentDn.remove( parentDn.size() - 1 );
        parentDn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );

        if ( !parentDn.equals( ouSchemaDN ) )
        {
            throw new LdapInvalidNameException( "The parent dn of a schema should be " + ouSchemaDN.getUpName() + " and not: "
                + parentDn.toNormName(), ResultCodeEnum.NAMING_VIOLATION );
        }

        // check if the new schema is enabled or disabled
        boolean isEnabled = false;
        EntryAttribute disabled = entry.get( disabledAT );
        
        if ( disabled == null )
        {
            // If the attribute is absent, then the schema is enabled by default
            isEnabled = true;
        }
        else if ( ! disabled.contains( "TRUE" ) )
        {
            isEnabled = true;
        }
        
        // check to see that all dependencies are resolved and loaded if this
        // schema is enabled, otherwise check that the dependency schemas exist
        checkForDependencies( isEnabled, entry );
        
        /*
         * There's a slight problem that may result when adding a metaSchema
         * object if the addition of the physical entry fails.  If the schema
         * is enabled when added in the condition tested below, that schema
         * is added to the global registries.  We need to add this so subsequent
         * schema entity additions are loaded into the registries as they are
         * added to the schema partition.  However if the metaSchema object 
         * addition fails then we're left with this schema object looking like
         * it is enabled in the registries object's schema hash.  The effects
         * of this are unpredictable.
         * 
         * This whole problem is due to the inability of these handlers to 
         * react to a failed operation.  To fix this we would need some way
         * for these handlers to respond to failed operations and revert their
         * effects on the registries.
         * 
         * TODO: might want to add a set of failedOnXXX methods to the adapter
         * where on failure the schema service calls the schema manager and it
         * calls the appropriate methods on the respective handler.  This way
         * the schema manager can rollback registry changes when LDAP operations
         * fail.
         */

        if ( isEnabled )
        {
            Schema schema = factory.getSchema( entry );
            registries.schemaLoaded( schema );
        }
    }


    /**
     * Called to react to the deletion of a metaSchema object.  This method
     * simply removes the schema from the loaded schema map of the global 
     * registries.  
     * 
     * @param name the dn of the metaSchema object being deleted
     * @param entry the attributes of the metaSchema object 
     */
    public void delete( ServerEntry entry, boolean cascade ) throws Exception
    {
        EntryAttribute cn = entry.get( cnAT );
        String schemaName = cn.getString();

        // Before allowing a schema object to be deleted we must check
        // to make sure it's not depended upon by another schema
        Set<String> dependents = loader.listDependentSchemaNames( schemaName );
        if ( ! dependents.isEmpty() )
        {
            throw new LdapOperationNotSupportedException(
                "Cannot delete schema that has dependents: " + dependents,
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        // no need to check if schema is enabled or disabled here
        // if not in the loaded set there will be no negative effect
        registries.schemaUnloaded( loader.getSchema( schemaName ) );
    }



    /**
     * Responds to the rdn (commonName) of the metaSchema object being 
     * changed.  Changes all the schema entities associated with the 
     * renamed schema so they now map to a new schema name.
     * 
     * @param name the dn of the metaSchema object renamed
     * @param entry the entry of the metaSchema object before the rename
     * @param newRdn the new commonName of the metaSchema object
     */
    public void rename( ServerEntry entry, Rdn newRdn, boolean cascade ) throws Exception
    {
        String rdnAttribute = newRdn.getUpType();
        String rdnAttributeOid = registries.getAttributeTypeRegistry().getOidByName( rdnAttribute );

        if ( ! rdnAttributeOid.equals( cnAT.getOid() ) )
        {
            throw new LdapOperationNotSupportedException( 
                "Cannot allow rename with rdnAttribute set to " 
                + rdnAttribute + ": cn must be used instead." ,
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        /*
         * This operation has to do the following:
         * 
         * [1] check and make sure there are no dependent schemas on the 
         *     one being renamed - if so an exception should result
         *      
         * [2] make non-schema object registries modify the mapping 
         *     for their entities: non-schema object registries contain
         *     objects that are not SchemaObjects and hence do not carry
         *     their schema within the object as a property
         *     
         * [3] make schema object registries do the same but the way
         *     they do them will be different since these objects will
         *     need to be replaced or will require a setter for the 
         *     schema name
         */
        
        // step [1]
        String schemaName = getSchemaName( entry.getDn() );
        Set<String> dependents = loader.listDependentSchemaNames( schemaName );
        if ( ! dependents.isEmpty() )
        {
            throw new LdapOperationNotSupportedException( 
                "Cannot allow a rename on " + schemaName + " schema while it has depentents.",
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        // check if the new schema is enabled or disabled
        boolean isEnabled = false;
        EntryAttribute disabled = entry.get( disabledAT );
        
        if ( disabled == null )
        {
            isEnabled = true;
        }
        else if ( ! disabled.get().equals( "TRUE" ) )
        {
            isEnabled = true;
        }

        if ( ! isEnabled )
        {
            return;
        }

        // do steps 2 and 3 if the schema has been enabled and is loaded
        
        // step [2] 
        String newSchemaName = ( String ) newRdn.getUpValue();
        registries.getComparatorRegistry().renameSchema( schemaName, newSchemaName );
        registries.getNormalizerRegistry().renameSchema( schemaName, newSchemaName );
        registries.getSyntaxCheckerRegistry().renameSchema( schemaName, newSchemaName );
        
        // step [3]
        renameSchema( registries.getAttributeTypeRegistry(), schemaName, newSchemaName );
        renameSchema( registries.getDitContentRuleRegistry(), schemaName, newSchemaName );
        renameSchema( registries.getDitStructureRuleRegistry(), schemaName, newSchemaName );
        renameSchema( registries.getMatchingRuleRegistry(), schemaName, newSchemaName );
        renameSchema( registries.getMatchingRuleUseRegistry(), schemaName, newSchemaName );
        renameSchema( registries.getNameFormRegistry(), schemaName, newSchemaName );
        renameSchema( registries.getObjectClassRegistry(), schemaName, newSchemaName );
        renameSchema( registries.getLdapSyntaxRegistry(), schemaName, newSchemaName );
    }
    

    /**
     * Moves are not allowed for metaSchema objects so this always throws an
     * UNWILLING_TO_PERFORM LdapException.
     */
    public void moveAndRename( LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn, 
        ServerEntry entry, boolean cascade ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Moving around schemas is not allowed.",
            ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    /**
     * Moves are not allowed for metaSchema objects so this always throws an
     * UNWILLING_TO_PERFORM LdapException.
     */
    public void move( LdapDN oriChildName, LdapDN newParentName, 
        ServerEntry entry, boolean cascade ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Moving around schemas is not allowed.",
            ResultCodeEnum.UNWILLING_TO_PERFORM );
    }

    
    // -----------------------------------------------------------------------
    // private utility methods
    // -----------------------------------------------------------------------

    
    private boolean disable( LdapDN name, ModificationOperation modOp, EntryAttribute disabledInMods, EntryAttribute disabledInEntry )
        throws Exception
    {
        switch ( modOp )
        {
            /*
             * If the user is adding a new m-disabled attribute to an enabled schema, 
             * we check that the value is "TRUE" and disable that schema if so.
             */
            case ADD_ATTRIBUTE :
                if ( disabledInEntry == null )
                {
                    if ( "TRUE".equalsIgnoreCase( disabledInMods.getString() ) )
                    {
                        return disableSchema( getSchemaName( name ) );
                    }
                }
                
                break;

            /*
             * If the user is removing the m-disabled attribute we check if the schema is currently 
             * disabled.  If so we enable the schema.
             */
            case REMOVE_ATTRIBUTE :
                if ( ( disabledInEntry != null ) && ( "TRUE".equalsIgnoreCase( disabledInEntry.getString() ) ) )
                {
                    return enableSchema( getSchemaName( name ) );
                }
                
                break;

            /*
             * If the user is replacing the m-disabled attribute we check if the schema is 
             * currently disabled and enable it if the new state has it as enabled.  If the
             * schema is not disabled we disable it if the mods set m-disabled to true.
             */
            case REPLACE_ATTRIBUTE :
                
                boolean isCurrentlyDisabled = false;
                
                if ( disabledInEntry != null )
                {
                    isCurrentlyDisabled = "TRUE".equalsIgnoreCase( disabledInEntry.getString() );
                }
                
                boolean isNewStateDisabled = false;
                
                if ( disabledInMods != null )
                {
                    isNewStateDisabled = "TRUE".equalsIgnoreCase( disabledInMods.getString() );
                }

                if ( isCurrentlyDisabled && !isNewStateDisabled )
                {
                    return enableSchema( getSchemaName( name ) );
                }

                if ( !isCurrentlyDisabled && isNewStateDisabled )
                {
                    return disableSchema( getSchemaName( name ) );
                }
                
                break;
                
            default:
                throw new IllegalArgumentException( "Unknown modify operation type: " + modOp );
        }
        
        return SCHEMA_UNCHANGED;
    }


    private String getSchemaName( LdapDN schema )
    {
        return ( String ) schema.getRdn().getValue();
    }


    private boolean disableSchema( String schemaName ) throws Exception
    {
        // First check that the schema is not already disabled
        Map<String, Schema> schemas = registries.getLoadedSchemas();
        
        Schema schema = schemas.get( schemaName );
        
        if ( ( schema == null ) || schema.isDisabled() )
        {
            // The schema is disabled, do nothing
            return SCHEMA_UNCHANGED;
        }
        
        Set<String> dependents = loader.listEnabledDependentSchemaNames( schemaName );
        
        if ( ! dependents.isEmpty() )
        {
            throw new LdapOperationNotSupportedException(
                "Cannot disable schema with enabled dependents: " + dependents,
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        schema.disable();
        
        // @TODO elecharny
        
        if ( "blah".equals( "blah" ) )
        {
            throw new NotImplementedException( "We have to disable the schema on partition" +
                    " and we have to implement the unload method below." );
        }
        
        // registries.unload( schemaName );
        
        return SCHEMA_MODIFIED;
    }


    /**
     * TODO - for now we're just going to add the schema to the global 
     * registries ... we may need to add it to more than that though later.
     */
    private boolean enableSchema( String schemaName ) throws Exception
    {
        Schema schema = loader.getSchema( schemaName );

        if ( schema != null )
        {
            // TODO log warning: schemaName + " was already loaded"
            schema.enable();
            registries.schemaLoaded( schema );
            return SCHEMA_UNCHANGED;
        }

        loader.loadWithDependencies( schema, registries );
        schema = loader.getSchema( schemaName );
        schema.enable();
        
        return SCHEMA_MODIFIED;
    }


    /**
     * Checks to make sure the dependencies either exist for disabled metaSchemas,
     * or exist and are loaded (enabled) for enabled metaSchemas.
     * 
     * @param isEnabled whether or not the new metaSchema is enabled
     * @param entry the Attributes for the new metaSchema object
     * @throws NamingException if the dependencies do not resolve or are not
     * loaded (enabled)
     */
    private void checkForDependencies( boolean isEnabled, ServerEntry entry ) throws Exception
    {
        EntryAttribute dependencies = entry.get( this.dependenciesAT );

        if ( dependencies == null )
        {
            return;
        }
        
        if ( isEnabled )
        {
            // check to make sure all the dependencies are also enabled
            Map<String,Schema> loaded = registries.getLoadedSchemas();
            
            for ( Value<?> value:dependencies )
            {
                String dependency = value.getString();
                
                if ( ! loaded.containsKey( dependency ) )
                {
                    throw new LdapOperationNotSupportedException( 
                        "Unwilling to perform operation on enabled schema with disabled or missing dependencies: " 
                        + dependency, ResultCodeEnum.UNWILLING_TO_PERFORM );
                }
            }
        }
        else
        {
            Set<String> allSchemas = loader.getSchemaNames();
            
            for ( Value<?> value:dependencies )
            {
                String dependency = value.getString();
                
                if ( ! allSchemas.contains( dependency ) )
                {
                    throw new LdapOperationNotSupportedException( 
                        "Unwilling to perform operation on schema with missing dependencies: " + dependency, 
                        ResultCodeEnum.UNWILLING_TO_PERFORM );
                }
            }
        }
    }

    
    /**
     * Used to iterate through SchemaObjects in a SchemaObjectRegistry and rename
     * their schema property to a new schema name.
     * 
     * @param registry the registry whose objects are changed
     * @param originalSchemaName the original schema name
     * @param newSchemaName the new schema name
     */
    private void renameSchema( SchemaObjectRegistry<? extends SchemaObject> registry, String originalSchemaName, String newSchemaName ) 
    {
        Iterator<? extends SchemaObject> list = registry.iterator();
        while ( list.hasNext() )
        {
            SchemaObject obj = list.next();
            if ( obj.getSchemaName().equalsIgnoreCase( originalSchemaName ) )
            {
                obj.setSchemaName( newSchemaName );
            }
        }
    }
}
