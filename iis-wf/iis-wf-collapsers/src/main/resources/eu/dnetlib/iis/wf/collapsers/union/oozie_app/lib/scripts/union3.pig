SET mapred.child.java.opts $mapred_child_java_opts

define avro_load_input AvroStorage('$schema_input');

define avro_store_output AvroStorage('$schema_output');

input1 = load '$input_1' using avro_load_input;
input2 = load '$input_2' using avro_load_input;
input3 = load '$input_3' using avro_load_input;

input_union = union input1, input2, input3;

store input_union into '$output' using avro_store_output;