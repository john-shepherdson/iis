#!/bin/bash

# #############################################################################################################
# prerequisites (for the most common deployment scenario: IIS primary wf deployment):
# 1) having wf/primary/main worfklow already deployed on HDFS in a standard directory within user directory
# 2) document-similarity-s1-rank_filter.pig patched file in current directory
# #############################################################################################################

# exitting on error
set -e
# printing commands
set -x


export deployment_date=`date +%Y-%m-%d`

declare -a app_dirs=(
  #"wf/referenceextraction/softwareurl/cache/builder"
  #"wf/metadataextraction/cache/builder"
  "wf/primary/main"
)

declare -a hdfs_target_dirs=(
  #"/lib/iis/softwareurl_cache_builder/snapshots"
  #"/lib/iis/cache_builder/snapshots"
  "/tmp/jenkins/snapshots" #"/lib/iis/primary/snapshots"
)

# gaining hdfs priviledges first in order to deploy oozie workflows to /lib/iis/ HDFS dir
user_name=`whoami`
export HADOOP_USER_NAME="hdfs"

for (( i=0; i<${#app_dirs[@]}; i++ ));
do
  # creating config-default.xml files in approparite local FS directories first
  local_cfg_root_dir=${app_dirs[$i]}'/config-default'
  mkdir ${local_cfg_root_dir}/${deployment_date}
  envsubst < ${local_cfg_root_dir}/config-default.template > ${local_cfg_root_dir}/${deployment_date}/config-default.xml

  hdfs_app_source_dir='/user/'$user_name'/'${app_dirs[$i]}'/oozie_app'
  fs_cfg_source_dir=${local_cfg_root_dir}/${deployment_date}
  hdfs_target_dir=${hdfs_target_dirs[$i]}/${deployment_date}

  # deploying app
  hadoop fs -mv ${hdfs_app_source_dir} ${hdfs_target_dir}

  # deploying config
  hadoop fs -put ${fs_cfg_source_dir}/config-default.xml ${hdfs_target_dir}
done

echo APPLYING MANUAL DOCSIM PATCH 2976...
script_name='document-similarity-s1-rank_filter.pig'
# remote_script_location='/lib/iis/primary/snapshots/'${deployment_date}'/primary_processing/documentssimilarity_chain/coansys/pl.edu.icm.coansys-document-similarity-ranked-workflow'
remote_script_location='/tmp/jenkins/snapshots/'${deployment_date}'/primary_processing/documentssimilarity_chain/coansys/pl.edu.icm.coansys-document-similarity-ranked-workflow'

hadoop fs -rm $remote_script_location/$script_name
hadoop fs -put $script_name $remote_script_location

export HADOOP_USER_NAME="$user_name"
