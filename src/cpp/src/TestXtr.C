#include <cxxtest/TestSuite.h>
#include <typeinfo>
#include <iostream>
#include "Xtr.h"
using namespace std;
using namespace xtr;
         
    /* Test Metadata */
    /* Several xtr_metadata instances to test different aspects */
 
    u_int8_t t1[8] = {0,1,2,3,4,5,6,7}; 
    u_int8_t t2[4] = {3,3,3,3};
    //no options
    u_int8_t m1[9] = {0,0,0,0,0,0,0,0,0};         //ok, i
    u_int8_t m1v1[9] = {0x10,0,0,0,0,0,0,0,0};
    u_int8_t m1v1o8[13] = {0x18,0,0,0,0,0,0,0,0,0,0,0,0};
    char     s1[19]= "000000000000000000";
    u_int8_t m2[3] = {0,0,0};                     //x 
    char     s2[7] = "000000";
    u_int8_t m3[9] = {0,0,1,2,3,3,3,3,4};         //ok, v
    char     s3[19]= "000001020303030304";         //ok, v
    //taskId 8
    u_int8_t m4[13] = {1,0,1,2,3,4,5,6,7,3,3,3,3}; //ok, v
    u_int8_t m5[13] = {1,0,0,0,0,0,0,0,0,3,3,3,3}; //ok, i
    u_int8_t m6[9] = {1,0,1,2,3,3,3,3,3};         //x (no opId)
    //taskId 12
    u_int8_t m7[17] = {2,0,1,2,3,4,5,6,7,8,9,10,11,3,3,3,3};   //ok, v
    char     s7[35] = "02000102030405060708090A0B03030303";    
    char    sl7[35] = "02000102030405060708090a0b03030303";    //lower case
    char     x7[36] = "02000102030405060708090a0b030303033";   //odd 
    char     y7[35] = "02000102030405060708090x0b03030303";   //wrong char 
    u_int8_t m8[17] = {2,0,0,0,0,0,0,0,0,0,0, 0, 0,3,3,3,3};   //ok, i
    u_int8_t m9[18] = {2,0,1,2,3,4,5,6,7,8,9,10,11,3,3,3,3,5}; //ok, will ignore trailing 5
    //taskId 20 
    u_int8_t m10[25] = {3,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,4,4,4,4}; //ok, v
    u_int8_t m11[25] = {3,0,0,0,0,0,0,0,0,0,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,4,4,4,4}; //ok, i
    u_int8_t m12[13] = {3,0,1,2,3,4,5,6,7,8,10,11};  //x, insufficient space
    
    //with options. These options conform to the metadata spec, but not 
    //              to the option types spec (i.e., they can be parsed, but do not
    //              mean the right thing)
    u_int8_t m20[11] = {4,0,1,2,3,2,2,2,2,1,0};   //1 opt, no-op
    u_int8_t m21[12] = {4,0,1,2,3,1,1,1,1,2,1,0}; //1 opt, type 1, length 0
    u_int8_t m22[16] = {4,0,1,2,3,1,1,1,1,6,0,0,0,0,0,0}; //6 no-ops, padding
    u_int8_t m23[24] = {4,0,1,2,3,1,1,1,1,14,5,3,1,2,3,6,4,1,2,3,4,0,0,0}; //2 opts, 3 no-ops
    char     s23[49] = "0400010203010101010E0503010203060401020304000000"; //2 opts, 3 no-ops
    //1 opt, 1 no-op. Should ignore other opts ater no-op and convert them to no-ops
    u_int8_t m24[21] = {4,0,1,2,3,1,1,1,1,11,5,3,1,2,3,0,5,3,1,2,3};
    u_int8_t c24[21] = {4,0,1,2,3,1,1,1,1,11,5,3,1,2,3,0,0,0,0,0,0};  
  
    //strange flags
    u_int8_t m30[9] = {0x10,0,1,2,3,10,10,10,10};  //version is 1. ok
    u_int8_t m31[9] = {0x08,0,1,2,3,10,10,10,10};  //bit 4 is set. this will break because opid is not 8 long
    //this implem. will ignore, and store this instead
    u_int8_t c31[9] = {0x10,0,0,0,0,0,0,0,0};  
  
    u_int8_t m32[9] = {4,0,1,2,3,10,10,10,10};     //opt bit is set but there are no options!

    u_int8_t m33[17]  = {4,0,1,2,3,10,10,10,10,07,0xCE,01,07,0xC1,02,0xF8,0xE6};
    
    u_int8_t m34v1[13]    = {0x18,0,1,2,3,1,2,3,4,5,6,7,8}; //version 1, 8-byte opid
    u_int8_t m35v1[13]    = {0x08,0,1,2,3,1,2,3,4,5,6,7,8}; //version 0, 8-byte opid. This impl.
                                                            //will accept and set the version to
                                                            //1.
    u_int8_t m36v1[9]     = {0x20,0,1,2,3,1,2,3,4}; //bad version, invalid
    u_int8_t m37v1[17]      = {0x19,0,1,2,3,4,5,6,7,0,1,2,3,4,5,6,7};//opId 8, taskId 8
    char     s37v1[35]      = "1900010203040506070001020304050607";
    u_int8_t m38v1[16]      = {0x1C,0,1,2,3,0,1,2,3,4,5,6,7,2,1,0}; //opId 8, 1 opt


class XtrMetadataTestSuite : public CxxTest::TestSuite
{
public:
    void testXtrOption() 
    {

        /* The type 33 here is assumed to be UNKNOWN in the tests
         * below. The well-formed options of this type below will
         * be converted into OptionAny objects */

        u_int8_t ob1[1] = {0};     //Nop,right
        u_int8_t ob2[2] = {0,0};   //Nop,will only read 1
        u_int8_t ob3[3] = {0,1,1}; //Nop,will only read 1
        u_int8_t ob4[2] = {33,0};   //right
        u_int8_t ob5[3] = {33,4,1}; //wrong: invalid size:
        u_int8_t ob6[6] = {33,4,1,2,3,4};      //right
        u_int8_t ob7[8] = {33,5,1,2,3,4,5,6};  //right, will not read all
        u_int8_t ob8[2] = {33,254};            //wrong, length is invalid
        u_int8_t ob_payload1[5] = {1,2,3,4,5};
        u_int8_t op[10];


        size_t s,sp;
        Option *o1;

        s = 1;
        o1 = Option::createFromBytes(ob1, &s);        
        TS_ASSERT_EQUALS(s, 1u);
        TS_ASSERT_EQUALS(o1->getType(), 0u); 
        TS_ASSERT_EQUALS(o1->getLength(), 0u);
        TS_ASSERT(typeid(*o1) == typeid(OptionNop));
        sp = sizeof(op);
        o1->pack(op, &sp);
        TS_ASSERT_EQUALS(sp, 1u);
        TS_ASSERT_SAME_DATA(op, ob1, sp);
        delete o1;

        s = 2;
        o1 = Option::createFromBytes(ob2, &s);        
        TS_ASSERT_EQUALS(s, 1u);
        TS_ASSERT_EQUALS(o1->getType(), 0u);
        TS_ASSERT_EQUALS(o1->getLength(), 0u);
        TS_ASSERT(typeid(*o1) == typeid(OptionNop));
        sp = sizeof(op);
        o1->pack(op, &sp);
        TS_ASSERT_EQUALS(sp, 1u);
        TS_ASSERT_SAME_DATA(op, ob1, sp);
        delete o1;

        s = 3;
        o1 = Option::createFromBytes(ob3, &s);        
        TS_ASSERT_EQUALS(s, 1u); 
        TS_ASSERT_EQUALS(o1->getType(), 0u);
        TS_ASSERT_EQUALS(o1->getLength(), 0u);
        TS_ASSERT(typeid(*o1) == typeid(OptionNop));
        sp = sizeof(op);
        o1->pack(op, &sp);
        TS_ASSERT_EQUALS(sp, 1u);
        TS_ASSERT_SAME_DATA(op, ob1, sp);
        delete o1;

        s = 2;
        o1 = Option::createFromBytes(ob4, &s);        
        TS_ASSERT_EQUALS(s, 2u); 
        TS_ASSERT_EQUALS(o1->getType(), 33u);
        TS_ASSERT_EQUALS(o1->getLength(), 0u);
        TS_ASSERT(typeid(*o1) == typeid(OptionAny));
        sp = sizeof(op);
        o1->pack(op, &sp);
        TS_ASSERT_EQUALS(sp, 2u);
        TS_ASSERT_SAME_DATA(op, ob4, sp);
        delete o1;

        s = 3;
        o1 = Option::createFromBytes(ob5, &s);        
        TS_ASSERT_EQUALS(s, 0u); 
        TS_ASSERT_EQUALS(o1->getType(), 0u);
        TS_ASSERT_EQUALS(o1->getLength(), 0u);
        TS_ASSERT(typeid(*o1) == typeid(OptionNop));
        delete o1;

        s = 6;
        o1 = Option::createFromBytes(ob6, &s);        
        TS_ASSERT_EQUALS(s, 6u); 
        TS_ASSERT_EQUALS(o1->getType(), 33u);
        TS_ASSERT_EQUALS(o1->getLength(), 4u);
        TS_ASSERT_SAME_DATA(((OptionAny*)o1)->getPayload(), ob_payload1, 4);
        TS_ASSERT(typeid(*o1) == typeid(OptionAny));
        sp = sizeof(op);
        o1->pack(op, &sp);
        TS_ASSERT_EQUALS(sp, 6u);
        TS_ASSERT_SAME_DATA(op, ob6, sp);
        delete o1;

        s = 8;
        o1 = Option::createFromBytes(ob7, &s);        
        TS_ASSERT_EQUALS(s, 7u); 
        TS_ASSERT_EQUALS(o1->getType(), 33u);
        TS_ASSERT_EQUALS(o1->getLength(), 5u);
        TS_ASSERT(typeid(*o1) == typeid(OptionAny));
        sp = sizeof(op);
        o1->pack(op, &sp);
        TS_ASSERT_EQUALS(sp, 7u);
        TS_ASSERT_SAME_DATA(op, ob7, sp);
        delete o1;
    
        s = 256;
        o1 = Option::createFromBytes(ob8, &s);        
        TS_ASSERT_EQUALS(s, 0u);
        TS_ASSERT_EQUALS(o1->getType(), 0u);
        TS_ASSERT_EQUALS(o1->getLength(), 0u);
        TS_ASSERT(typeid(*o1) == typeid(OptionNop));
        delete o1;
        
        o1 = new OptionNop();
        TS_ASSERT_EQUALS(o1->getType(), 0u);
        TS_ASSERT_EQUALS(o1->getLength(), 0u);
        delete o1;

        o1 = new OptionAny(0, 5, ob_payload1);
        TS_ASSERT_EQUALS(o1->getType(), 0u);
        TS_ASSERT_EQUALS(o1->getLength(), 0u);
        delete o1;

        o1 = new OptionAny(33, 5, ob_payload1);
        TS_ASSERT_EQUALS(o1->getType(), 33u);
        TS_ASSERT_EQUALS(o1->getLength(), 5u);
        TS_ASSERT_SAME_DATA((static_cast<OptionAny*>(o1))->getPayload(), ob_payload1, 5);
        sp = sizeof(op);
        o1->pack(op, &sp);
        TS_ASSERT_EQUALS(sp, 7u);
        TS_ASSERT_SAME_DATA(op, ob7, sp);
        delete o1;

        o1 = new OptionAny(0, 0, (u_int8_t*)0);
        TS_ASSERT_EQUALS(o1->getType(), 0u);
        TS_ASSERT_EQUALS(o1->getLength(), 0u);
        sp = sizeof(op);
        o1->pack(op, &sp);
        TS_ASSERT_EQUALS(sp, 1u);
        TS_ASSERT_SAME_DATA(op, ob1, sp);
        Option* o2 = new OptionNop();
        TS_ASSERT(o1->isEqual(*o2));
        delete o1;
        delete o2;

        o1 = new OptionAny(0, 1, (u_int8_t*)0);
        TS_ASSERT_EQUALS(o1->getType(), 0u);
        TS_ASSERT_EQUALS(o1->getLength(), 0u);
        delete o1;
    }

    void testXtrOptionChainId()
    {
        u_int8_t ob1[4] = {0xC1,2,0,0}; //won't pack to anything
        u_int8_t ob2[4] = {0xC1,2,0,3}; //chain id 0x0003

        u_int8_t ob4[4] = {1,2,3,3};    //wrong id
        u_int8_t ob5[5] = {0xC1,3,1,2,3}; //wrong length, size
        
        u_int8_t p[10];
        size_t s;
        
        Option *o1, *o2;
        OptionChainId *co;
        
        s = 4;
        o1 = Option::createFromBytes(ob1,&s);
        TS_ASSERT_EQUALS(s,4u);
        TS_ASSERT_EQUALS(o1->getType(), (u_int8_t)Option::CHAIN_ID);
        TS_ASSERT_EQUALS(o1->getLength(),2);
        TS_ASSERT_EQUALS(o1->getSize(),4);
        co = dynamic_cast<OptionChainId*>(o1);
        TS_ASSERT(co);
        TS_ASSERT_EQUALS(co->getId(),0); //valgrind doesn't like this
        o2 = new OptionChainId();
        TS_ASSERT(o1->isEqual(*o2));

        co->setId(3);
        TS_ASSERT_EQUALS(co->getId(),3);
        s = 10;
        TS_ASSERT_EQUALS(o1->pack(p,&s),XTR_SUCCESS);
        TS_ASSERT_EQUALS(s,4u);
        TS_ASSERT_SAME_DATA(p, ob2, s);

        delete o2;

        s = 4;
        o2 = new OptionAny(ob2,&s);
        TS_ASSERT(o1->isEqual(*o2));
    
        delete o1; //valgrind also doesn't like this
        s = 4;
        o1 = Option::createFromBytes(ob2,&(s=4));
        TS_ASSERT_EQUALS(co->getId(), 3);
        delete o1;

        s = 10;
        o1 = new OptionChainId(ob4,&(s=4));
        TS_ASSERT_EQUALS(s,0u);
        delete o1;

        o1 = Option::createFromBytes(ob5, &(s=5));
        TS_ASSERT(typeid(*o1) == typeid(OptionNop));
        delete o1;

        o1 = Option::createFromBytes(ob5, &(s=4));
        TS_ASSERT(typeid(*o1) == typeid(OptionNop));
        delete o1;
        delete o2;
        
    }
    
    void testXtrOptionSeverity()
    {
        u_int8_t ob1[3] = {0xCE,1,3};
        u_int8_t ob2[3] = {0xCE,1,OptionSeverity::DEBUG};
        u_int8_t ob3[3] = {0xCE,1,OptionSeverity::NOTICE};
        u_int8_t ob10[4] = {0xCE,2,1,1}; //error in size
        u_int8_t ob11[3] = {0xC,1,1}; //wrong option type

        Option *o1;
        OptionSeverity *lo;

        u_int8_t p[10];
        size_t s;
        
        s = 3;
        o1 = Option::createFromBytes(ob1, &s);
        TS_ASSERT_EQUALS(s,3u);
        TS_ASSERT_EQUALS(o1->getType(), (u_int8_t)Option::SEVERITY);
        TS_ASSERT_EQUALS(o1->getLength(),1);
        TS_ASSERT_EQUALS(o1->getSize(),3);
        lo = static_cast<OptionSeverity*>(o1);
        TS_ASSERT_EQUALS(lo->getSeverity(), 3);
        
        lo->setSeverity(5);
        TS_ASSERT_EQUALS(lo->getSeverity(), 5);
        lo->setSeverity(OptionSeverity::DEBUG);
        TS_ASSERT_EQUALS(lo->getSeverity(), OptionSeverity::DEBUG);
        TS_ASSERT_EQUALS(lo->getSeverity(), 7);

        lo = new OptionSeverity();
        TS_ASSERT_EQUALS(lo->getSeverity(), OptionSeverity::_DEFAULT);
        delete lo;

        s = 10;
        TS_ASSERT_EQUALS(o1->pack(p, &s), XTR_SUCCESS);
        TS_ASSERT_EQUALS(s,3u);
        TS_ASSERT_SAME_DATA(p, ob2, s);
        
        delete o1;

        o1 = new OptionSeverity(ob10, &(s=4));
        TS_ASSERT_EQUALS(s,0u);
        delete o1;
        
        o1 = Option::createFromBytes(ob10, &(s=4));
        TS_ASSERT(typeid(*o1) == typeid(OptionNop));
        delete o1;
       
        o1 = new OptionSeverity(ob11, &(s=3));
        TS_ASSERT_EQUALS(s,0u);
        lo = static_cast<OptionSeverity*>(o1);
        TS_ASSERT_EQUALS(lo->getSeverity(), OptionSeverity::NOTICE);
        delete o1;
    
        o1 = new OptionSeverity();
        s = 10;
        TS_ASSERT_EQUALS(o1->pack(p, &s), XTR_SUCCESS);
        TS_ASSERT_EQUALS(s,3u);
        TS_ASSERT_SAME_DATA(p, ob3, s);
        delete o1;
        
    }

    void testXtrId() 
    {
        u_int8_t i1[20] = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};
        u_int8_t i1c[20] = {1,2,3,4,5,6,7,0,9,10,11,12,13,14,15,16,17,18,19,20};
        u_int8_t b[20];
        TaskId *t1;
        size_t s = 20;

        t1 = new TaskId();
        TS_ASSERT_EQUALS(t1->getLength(),4u);
        t1->setBytes(i1, 20);
        TS_ASSERT_EQUALS(t1->getLength(),20u);
        t1->setLength(18);
        (*t1)[7] = 0;
        t1->pack(b, &s);
        TS_ASSERT_EQUALS(s,18u);
        TS_ASSERT_SAME_DATA(b, i1c, s);
        delete t1;
    }
   
    void testTaskIdFromBytes() {
      u_int8_t b0[4] = {0,0,0,0};
      u_int8_t b20[20] = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
      u_int8_t out[20];
      unsigned int valid_len[4] = {4,8,12,20};
      unsigned int invalid_len[4] = {0,5,11,19};
      size_t len;
      int i;
      TaskId* id;
      Id<20>* id2;
    
      id = new TaskId(b0,4);
      TS_ASSERT(!id->isValid());
      TS_ASSERT_EQUALS(id->getLength(), 4u);
      delete id;
      
      for (i = 0; i < 4; i++) {
        //create a new id with valid length
        id = new TaskId(b20, valid_len[i]);
        TS_ASSERT_EQUALS(id->getLength(), valid_len[i]);
        TS_ASSERT(id->isLengthValid());
        len = valid_len[i];
        //make sure the output of pack is the same as the input
        TS_ASSERT_EQUALS(id->pack(out, &len), XTR_SUCCESS);
        TS_ASSERT_EQUALS(len, valid_len[i]);
        TS_ASSERT_SAME_DATA(b20, out, valid_len[i]);
        //make sure we can't pack with a smaller length.
        len = invalid_len[i];
        TS_ASSERT_EQUALS(id->pack(out, &len), XTR_FAIL);
        //try to set bytes to an invalid length: should return fail 
        //and not change the id.
        id2 = id;
        TS_ASSERT_EQUALS(id->setBytes(b20, invalid_len[i]), XTR_FAIL); 
        TS_ASSERT_EQUALS(id2->setBytes(b20, invalid_len[i]), XTR_FAIL); 
        TS_ASSERT_EQUALS(id->setBytes(b20, valid_len[i]), XTR_SUCCESS); 
        TS_ASSERT_EQUALS(id2->setBytes(b20, valid_len[i]), XTR_SUCCESS); 
        delete id;
        //create with an invalid length: should instead create
        //with an invalid 4 bytes id
        id = new TaskId(invalid_len[i]);
        TS_ASSERT_DIFFERS(invalid_len[i], id->getLength());
        TS_ASSERT_EQUALS(id->getLength(), 4u);
        TS_ASSERT(!id->isValid());
        delete id;
        //create with an invalid length, from a buffer:
        //should create an invalid id of length 4
        id = new TaskId(b20, invalid_len[i]);
        TS_ASSERT_EQUALS(id->getLength(), 4u);
        TS_ASSERT(!id->isValid());
        delete id;
      }    
    }
    void testOpId() {
        OpId id;
        OpId id8(8);
        u_int8_t iv[8] = {0,0,0,0,0,0,0,0};
        u_int8_t  v[8] = {1,2,3,4,5,6,7,8};
        
        //default constructor: all 0's
        TS_ASSERT_SAME_DATA(id.getBytes(), iv, 4);
        //set bytes
        TS_ASSERT_EQUALS(id.setBytes(v, 4), XTR_SUCCESS);
        TS_ASSERT_SAME_DATA(id.getBytes(), v, 4);
        id = OpId(v, 4);
        TS_ASSERT_SAME_DATA(id.getBytes(), v, 4);
        id = OpId(v, 3);
        TS_ASSERT_EQUALS(id.getLength(), 4u);
        TS_ASSERT_SAME_DATA(id.getBytes(), iv, 4);
        //set bytes to invalid length
        TS_ASSERT_EQUALS(id.setBytes(v, 3), XTR_FAIL);
        //8 bytes
        TS_ASSERT_SAME_DATA(id8.getBytes(), iv, 8);
        TS_ASSERT_EQUALS(id8.getLength(), 8u);
        TS_ASSERT_EQUALS(id8.setBytes(v,8), XTR_SUCCESS);
        TS_ASSERT_SAME_DATA(id8.getBytes(), v, 8);
    }
    void testXtrTaskIdisEqual() {
        u_int8_t b0[20] = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
        u_int8_t b1[20] = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
        u_int8_t b2[20] = {1,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
        TaskId *id1, *id2;
        int len[4] = {4,8,12,20};
        int i;
        
        id1 = new TaskId(b1,4);
        id2 = new TaskId(b2,4);
        //different contents
        TS_ASSERT(!id1->isEqual(*id2));
        //different lengths
        TS_ASSERT_EQUALS(id2->setBytes(b1,8), XTR_SUCCESS);
        TS_ASSERT(!id1->isEqual(*id2)); 
        //same ids
        for (i = 0; i < 4; i++) {
            id1->setBytes(b1,len[i]);
            id2->setBytes(b0,len[i]);
            TS_ASSERT(id1->isEqual(*id2));
        }
        delete id1; delete id2;
    }
    void testXtrMetadatasizeInArray() {
       TS_ASSERT_EQUALS(Metadata::sizeInArray( NULL,9 ), 0u  );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m1,  8 ), 0u  );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m1,  10), 9u  );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m1,  9 ), 9u  );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m2,  3 ), 0u  );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m3,  9 ), 9u  );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m4,  13), 13u );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m4,  9 ), 0u  );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m5,  13), 13u );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m6,  9 ), 0u  );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m7,  17), 17u );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m8,  17), 17u );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m9,  18), 17u );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m10, 25), 25u );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m11, 25), 25u );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m12, 13), 0u  );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m20, 11), 11u );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m21, 12), 12u );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m22, 16), 16u );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m23, 24), 24u );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m24, 21), 21u );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m30, 9 ), 9u  );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m31, 9 ), 0u  );
       TS_ASSERT_EQUALS(Metadata::sizeInArray( m32, 9 ), 0u  );
    }
    void testXtrMetadataCreation() {
        Metadata x1;
        Metadata x2;
        u_int8_t b0[50];
        size_t len;
    
        //new should be invalid, all 0's
        x1 = Metadata();
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 0u);
        TS_ASSERT(!x1.isValid());
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_SAME_DATA(b0, m1v1, 9);

        x1 = Metadata(4); //ok
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOpId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 0u);
        TS_ASSERT(!x1.isValid());
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_SAME_DATA(b0, m1v1, 9);

        x1 = Metadata(8); //ok
        x1 = Metadata(9); //invalid
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOpId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 0u);
        TS_ASSERT(!x1.isValid());
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_SAME_DATA(b0, m1v1, 9);


        x1 = Metadata(4,4); //ok
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOpId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 0u);
        TS_ASSERT(!x1.isValid());
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_SAME_DATA(b0, m1v1, 9);

        x1 = Metadata(4,8); //ok
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOpId().getLength(), 8u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 0u);
        TS_ASSERT(!x1.isValid());
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_SAME_DATA(b0, m1v1o8, 13);

        x1 = Metadata(4,9); //invalid
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOpId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 0u);
        TS_ASSERT(!x1.isValid());
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_SAME_DATA(b0, m1v1, 9);

        x1 = Metadata(5,8); //invalid
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOpId().getLength(), 8u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 0u);
        TS_ASSERT(!x1.isValid());
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_SAME_DATA(b0, m1v1o8, 13);

        x1 = Metadata(5,5); //invalid
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOpId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 0u);
        TS_ASSERT(!x1.isValid());
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_SAME_DATA(b0, m1v1, 9);

        //test the createRandom
        x1 = Metadata::createRandom();
        x2 = Metadata::createRandom();
        //should be different (except in 2^32-1 times)
        TS_ASSERT_DIFFERS(memcmp(x1.getTaskId().getBytes(), x2.getTaskId().getBytes(), 4), 0);
        TS_ASSERT_DIFFERS(memcmp(x1.getOpId().getBytes(), x2.getOpId().getBytes(), 4), 0);

        //version 1
        x1 = Metadata::createRandom(4);
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOpId().getLength(), 4u);
        TS_ASSERT(x1.isValid());
        len = sizeof(b0);

        x1 = Metadata::createRandom(5);
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOpId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 0u);
        TS_ASSERT(!x1.isValid());
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_SAME_DATA(b0, m1v1, 9);


        x1 = Metadata::createRandom(8);
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 8u);
        TS_ASSERT_EQUALS(x1.getOpId().getLength(), 4u);
        TS_ASSERT(x1.isValid());
        len = sizeof(b0);


        x1 = Metadata::createRandom(4,4);
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOpId().getLength(), 4u);
        TS_ASSERT(x1.isValid());
        len = sizeof(b0);

        x1 = Metadata::createRandom(4,5);
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOpId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 0u);
        TS_ASSERT(!x1.isValid());
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_SAME_DATA(b0, m1v1, 9);


        x1 = Metadata::createRandom(4,8);
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOpId().getLength(), 8u);
        TS_ASSERT(x1.isValid());
        len = sizeof(b0);

        //create with taskId
        TaskId tid = TaskId(t1,8);
        x1 = Metadata(tid);
        x2 = Metadata(tid);
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 8u);
        //should have different opIds (except in 2**(-32) chances)
        TS_ASSERT_DIFFERS(memcmp(x1.getOpId().getBytes(), x2.getOpId().getBytes(), 4), 0);
        OpId oid = OpId(t2,4);
        x1 = Metadata(tid, oid); 
        TS_ASSERT_SAME_DATA(x1.getTaskId().getBytes(), t1, 8);
        TS_ASSERT_SAME_DATA(x1.getOpId().getBytes(), t2, 4);
    }   
    void testXtrMetadataCreateFromBytes() {
        Metadata x1;
        u_int8_t b0[50];
        size_t len;

        x1 = Metadata::createFromBytes(NULL, 9);
        TS_ASSERT(!x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(len, x1.sizeAsBytes());
        TS_ASSERT_EQUALS(len, 9u);
        TS_ASSERT_SAME_DATA(b0, m1v1, len);

        //invalid length
        x1 = Metadata::createFromBytes(m3, 8u);
        TS_ASSERT(!x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(len, x1.sizeAsBytes());
        TS_ASSERT_EQUALS(len, 9u);
        TS_ASSERT_SAME_DATA(b0, m1v1, len);

        //ok length, invalid metadata "image"
        x1 = Metadata::createFromBytes(m1,9u);
        TS_ASSERT(!x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(len, x1.sizeAsBytes());
        TS_ASSERT_EQUALS(len, 9u);
        TS_ASSERT_SAME_DATA(b0, m1, len);
    
        //ok metadata, too short length
        x1 = Metadata::createFromBytes(m3, 3);
        TS_ASSERT(!x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(len, x1.sizeAsBytes());
        TS_ASSERT_EQUALS(len, 9u);
        TS_ASSERT_SAME_DATA(b0, m1v1, len);
    
        //ok length, ok metadata
        x1 = Metadata::createFromBytes(m3,9);
        TS_ASSERT(x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(len, x1.sizeAsBytes());
        TS_ASSERT_EQUALS(len, 9u);
        TS_ASSERT_SAME_DATA(b0, m3, len);
        
        //ok metadata, longer length: should ignore extra bytes 
        x1 = Metadata::createFromBytes(m3,10);
        TS_ASSERT(x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(len, x1.sizeAsBytes());
        TS_ASSERT_EQUALS(len, 9u);
        TS_ASSERT_SAME_DATA(b0, m3, len);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 0u);

        //other length: 8
        x1 = Metadata::createFromBytes(m4, 13);
        TS_ASSERT(x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 8u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(len, x1.sizeAsBytes());
        TS_ASSERT_EQUALS(len, 13u);
        TS_ASSERT_SAME_DATA(b0, m4, len);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 0u);

        //length 8, inalid taskId, valid opId
        x1 = Metadata::createFromBytes(m5, 13);
        TS_ASSERT(!x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 8u);
        TS_ASSERT_SAME_DATA(x1.getOpId().getBytes(), m5+9, 4);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(len, x1.sizeAsBytes());
        TS_ASSERT_EQUALS(len, 13u);
        TS_ASSERT_SAME_DATA(b0, m5, len);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 0u);

        //no op id
        x1 = Metadata::createFromBytes(m6, 9);
        TS_ASSERT(!x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);

        //task id length 12
        x1 = Metadata::createFromBytes(m7, 17);
        TS_ASSERT(x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 12u);
        TS_ASSERT_SAME_DATA(x1.getOpId().getBytes(), m7+13, 4);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(len, x1.sizeAsBytes());
        TS_ASSERT_EQUALS(len, 17u);
        TS_ASSERT_SAME_DATA(b0, m7, len);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 0u);

        //task id length 12, invalid id
        x1 = Metadata::createFromBytes(m8, 17);
        TS_ASSERT(!x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 12u);
        TS_ASSERT_SAME_DATA(x1.getOpId().getBytes(), m8+13, 4);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(len, x1.sizeAsBytes());
        TS_ASSERT_EQUALS(len, 17u);
        TS_ASSERT_SAME_DATA(b0, m8, len);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 0u);

        //task id 20
        x1 = Metadata::createFromBytes(m10, 25);
        TS_ASSERT(x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 20u);
        TS_ASSERT_SAME_DATA(x1.getOpId().getBytes(), m10+21, 4);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(len, x1.sizeAsBytes());
        TS_ASSERT_EQUALS(len, 25u);
        TS_ASSERT_SAME_DATA(b0, m10, len);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 0u);

        //task id 20, invalid id
        x1 = Metadata::createFromBytes(m11, 25);
        TS_ASSERT(!x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 20u);
        TS_ASSERT_SAME_DATA(x1.getOpId().getBytes(), m11+21, 4);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(len, x1.sizeAsBytes());
        TS_ASSERT_EQUALS(len, 25u);
        TS_ASSERT_SAME_DATA(b0, m11, len);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 0u);
       
        //task id 20, insufficient space 
        x1 = Metadata::createFromBytes(m12, 13);
        TS_ASSERT(!x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(len, x1.sizeAsBytes());
        TS_ASSERT_EQUALS(len, 9u);
        TS_ASSERT_SAME_DATA(b0, m1, len);
        
        //version 1
        x1 = Metadata::createFromBytes(m30, 9);
        TS_ASSERT(x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(len, x1.sizeAsBytes());
        TS_ASSERT_EQUALS(len, 9u);
        TS_ASSERT_SAME_DATA(b0, m30, len);
        
        //bit 4 set, but opId length is not 8
        x1 = Metadata::createFromBytes(m31, 9);
        TS_ASSERT(!x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(x1.sizeAsBytes(), 9u);
        TS_ASSERT_EQUALS(len, 9u);
        TS_ASSERT_SAME_DATA(b0, m1v1, len);
    }

    void testXtrMetadataCreateFromBytesV1() 
    {
        Metadata x1;
        u_int8_t b0[50];
        size_t len;

        //version 1, 8-byte opId
        x1 = Metadata::createFromBytes(m34v1, 13);
        TS_ASSERT(x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOpId().getLength(), 8u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(x1.sizeAsBytes(), 13u);
        TS_ASSERT_EQUALS(len, 13u);
        TS_ASSERT_SAME_DATA(b0, m34v1, len);

        //version 0, 8-byte opId: will set version to 1
        x1 = Metadata::createFromBytes(m35v1, 13);
        TS_ASSERT(x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOpId().getLength(), 8u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(x1.sizeAsBytes(), 13u);
        TS_ASSERT_EQUALS(len, 13u);
        TS_ASSERT_SAME_DATA(b0, m34v1, len);

        //bad version, invalid
        x1 = Metadata::createFromBytes(m36v1, 9);
        TS_ASSERT(!x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(x1.sizeAsBytes(), 9u);
        TS_ASSERT_EQUALS(len, 9u);
        TS_ASSERT_SAME_DATA(b0, m1, len);

        //version 1, 8-byte opId and TaskId
        x1 = Metadata::createFromBytes(m37v1, 17);
        TS_ASSERT(x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 8u);
        TS_ASSERT_EQUALS(x1.getOpId().getLength(), 8u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(x1.sizeAsBytes(), 17u);
        TS_ASSERT_EQUALS(len, 17u);
        TS_ASSERT_SAME_DATA(b0, m37v1, len);

        //version 1, 8-byte opId and 1 option
        x1 = Metadata::createFromBytes(m38v1, 16);
        TS_ASSERT(x1.isValid());
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOpId().getLength(), 8u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 1u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 2u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(x1.sizeAsBytes(), 16u);
        TS_ASSERT_EQUALS(len, 16u);
        TS_ASSERT_SAME_DATA(b0, m38v1, len);



    }

    void testXtrMetadataCreateFromBytesWithOptions() 
    {
        Metadata x1;
        u_int8_t b0[50];
        size_t len;

        x1 = Metadata::createFromBytes(m20, 11);
        TS_ASSERT(x1.isValid());
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 1u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 1u);
        TS_ASSERT_EQUALS(x1.getOptions()[0].getType(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions()[0].getLength(), 0u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(len, x1.sizeAsBytes());
        TS_ASSERT_EQUALS(len, 11u);
        TS_ASSERT_SAME_DATA(b0, m20, len);
        

        //1 opt, type 1, length 0
        x1 = (Metadata::createFromBytes(m21, 12));
        TS_ASSERT(x1.isValid());
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 1u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 2u);
        TS_ASSERT_EQUALS(x1.getOptions()[0].getType(), 1u);
        TS_ASSERT_EQUALS(x1.getOptions()[0].getLength(), 0u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(len, x1.sizeAsBytes());
        TS_ASSERT_EQUALS(len, 12u);
        TS_ASSERT_SAME_DATA(b0, m21, len);

        //6 opts, type 0, padding
        x1 = (Metadata::createFromBytes(m22, 16));
        TS_ASSERT(x1.isValid());
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 6u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 6u);
        TS_ASSERT_EQUALS(x1.getOptions()[0].getType(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions()[0].getLength(), 0u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(len, x1.sizeAsBytes());
        TS_ASSERT_EQUALS(len, 16u);
        TS_ASSERT_SAME_DATA(b0, m22, len);

        //2 opts, 3 no-ops
        u_int8_t buf[10];
        size_t sz = 10;
        x1 = (Metadata::createFromBytes(m23, 24));
        TS_ASSERT(x1.isValid());
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 5u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 14u);
        TS_ASSERT_EQUALS(x1.getOptions()[0].getType(), 5u);
        TS_ASSERT_EQUALS(x1.getOptions()[0].getLength(), 3u);
        TS_ASSERT_EQUALS(x1.getOptions()[1].pack(buf, &sz), XTR_SUCCESS);
        TS_ASSERT_SAME_DATA(buf, m23+15, 6);
        TS_ASSERT_EQUALS(x1.getOptions()[2].getType(), 0u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(len, x1.sizeAsBytes());
        TS_ASSERT_EQUALS(len, 24u);
        TS_ASSERT_SAME_DATA(b0, m23, len);

        //2 opts, 3 no-ops
        x1 = (Metadata::createFromBytes(m24, 21));
        TS_ASSERT(x1.isValid());
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 7u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 11u);
        TS_ASSERT_EQUALS(x1.getOptions()[0].getType(), 5u);
        TS_ASSERT_EQUALS(x1.getOptions()[0].getLength(), 3u);
        TS_ASSERT_EQUALS(x1.getOptions()[1].getType(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions()[2].getType(), 0u);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_EQUALS(len, x1.sizeAsBytes());
        TS_ASSERT_EQUALS(len, 21u);
        TS_ASSERT_SAME_DATA(b0, c24, len); //it should be different from m24


        //says there are options, but there's no space: should return
        //  metadata with no options.
        x1 = (Metadata::createFromBytes(m32, 9));
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 0u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 0u);
        TS_ASSERT(!x1.isValid());
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_SAME_DATA(b0, m1, 9);

        //metadata with 2 options, one of type Severity and another of type ChainId
        x1 = (Metadata::createFromBytes(m33,17));
        TS_ASSERT_EQUALS(x1.getTaskId().getLength(), 4u);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 2u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 7u);
        TS_ASSERT_EQUALS(x1.getSeverityThreshold(), 7);
        TS_ASSERT_EQUALS(x1.getChainId(), 0xF8E6);
        len = sizeof(b0);
        x1.pack(b0, &len);
        TS_ASSERT_SAME_DATA(b0, m33, 17);
    }

    //isEqual
    void testXtrIsEqual()
    {
        Metadata x1, x2;
        x1 = Metadata::createFromBytes(m1,9);
        x2 = Metadata::createFromBytes(m1,9);
        TS_ASSERT(x1.isEqual(x2)); TS_ASSERT(x2.isEqual(x1));
        TS_ASSERT(x1.getTaskId().isEqual(x2.getTaskId()));
        x1 = (Metadata::createFromBytes(m3,9));
        TS_ASSERT(!x1.isEqual(x2));
        x2 = (Metadata::createFromBytes(m3,9));
        TS_ASSERT(x1.isEqual(x2)); TS_ASSERT(x2.isEqual(x1));
        x1 = (Metadata::createFromBytes(m4,13));
        TS_ASSERT(!x1.isEqual(x2));
        x2 = (Metadata::createFromBytes(m4,13));
        TS_ASSERT(x1.isEqual(x2)); TS_ASSERT(x2.isEqual(x1));
        x1 = (Metadata::createFromBytes(m5,13));
        TS_ASSERT(!x1.isEqual(x2));
        x2 = (Metadata::createFromBytes(m5,13));
        TS_ASSERT(x1.isEqual(x2)); TS_ASSERT(x2.isEqual(x1));
        x1 = (Metadata::createFromBytes(m7,17));
        TS_ASSERT(!x1.isEqual(x2));
        x2 = (Metadata::createFromBytes(m7,17));
        TS_ASSERT(x1.isEqual(x2)); TS_ASSERT(x2.isEqual(x1));
        x2 = (Metadata::createFromBytes(m9,18));
        TS_ASSERT(x1.isEqual(x2)); TS_ASSERT(x2.isEqual(x1));
        x1 = (Metadata::createFromBytes(m8,17));
        TS_ASSERT(!x1.isEqual(x2));
        x2 = (Metadata::createFromBytes(m8,17));
        TS_ASSERT(x1.isEqual(x2)); TS_ASSERT(x2.isEqual(x1));
        x1 = (Metadata::createFromBytes(m10,25));
        TS_ASSERT(!x1.isEqual(x2));
        x2 = (Metadata::createFromBytes(m10,25));
        TS_ASSERT(x1.isEqual(x2)); TS_ASSERT(x2.isEqual(x1));
        x1 = (Metadata::createFromBytes(m11,25));
        TS_ASSERT(!x1.isEqual(x2));
        x2 = (Metadata::createFromBytes(m11,25));
        TS_ASSERT(x1.isEqual(x2)); TS_ASSERT(x2.isEqual(x1));
        x1 = (Metadata::createFromBytes(m20,11));
        TS_ASSERT(!x1.isEqual(x2));
        x2 = (Metadata::createFromBytes(m20,11));
        TS_ASSERT(x1.isEqual(x2)); TS_ASSERT(x2.isEqual(x1));
        x1 = (Metadata::createFromBytes(m21,12));
        TS_ASSERT(!x1.isEqual(x2));
        x2 = (Metadata::createFromBytes(m21,12));
        TS_ASSERT(x1.isEqual(x2)); TS_ASSERT(x2.isEqual(x1));
        x1 = (Metadata::createFromBytes(m22,16));
        TS_ASSERT(!x1.isEqual(x2));
        x2 = (Metadata::createFromBytes(m22,16));
        TS_ASSERT(x1.isEqual(x2)); TS_ASSERT(x2.isEqual(x1));
        x1 = (Metadata::createFromBytes(m23,24));
        TS_ASSERT(!x1.isEqual(x2));
        x2 = (Metadata::createFromBytes(m23,24));
        TS_ASSERT(x1.isEqual(x2)); TS_ASSERT(x2.isEqual(x1));
        x1 = (Metadata::createFromBytes(m24,21));
        TS_ASSERT(!x1.isEqual(x2));
        x2 = (Metadata::createFromBytes(m24,21));
        TS_ASSERT(x1.isEqual(x2)); TS_ASSERT(x2.isEqual(x1));
        x2 = (Metadata::createFromBytes(c24,21));
        TS_ASSERT(x1.isEqual(x2)); TS_ASSERT(x2.isEqual(x1));
    }

    void testXtrCopy()
    {
        Metadata x2, x3, *xp;
        OptionAny o;
        u_int8_t payload[4] = {192,168,1,1};

        o.setType(10);
        o.setLength(4);
        memcpy(o.getPayload(), payload, 4);

        Metadata x1 = (Metadata::createFromBytes(m23, 24));
        x3 = (Metadata::createFromBytes(m23, 24));
        x2 = x1; //assignment
        xp = new Metadata(x1); //copy constructor

        //the copies should be equal
        TS_ASSERT(x1.isEqual(x2));
        TS_ASSERT(x1.isEqual(*xp));

        x2.addOption(o);
        xp->getOptions().removeOptionAt(1);

        //not anymore...
        TS_ASSERT(!x1.isEqual(x2));
        TS_ASSERT(!x1.isEqual(*xp));
        //x1 should not have changed either...
        TS_ASSERT(x1.isEqual(x3));

        x1 = x2;
        TS_ASSERT(x1.isEqual(x2));

        delete xp;
    }

    //createFromString
    void testXtrCreateFromString() 
    {
        Metadata x1, x2;
        x1 = (Metadata::createFromBytes(m1,9));
        x2 = (Metadata::createFromString(s1,18));
        TS_ASSERT(x1.isEqual(x2));
        x1 = (Metadata::createFromBytes(m3,9));
        x2 = (Metadata::createFromString(s3,18));
        TS_ASSERT(x1.isEqual(x2));
        x1 = (Metadata::createFromBytes(m7,17));
        x2 = (Metadata::createFromString(s7,34));
        TS_ASSERT(x1.isEqual(x2));
        x2 = (Metadata::createFromString(sl7,34)); //lower case
        TS_ASSERT(x1.isEqual(x2));

        x1 = Metadata();
        x2 = (Metadata::createFromString(x7,35)); //odd
        TS_ASSERT(x1.isEqual(x2));
        x2 = (Metadata::createFromString(y7,35)); //invalid char
        TS_ASSERT(x1.isEqual(x2));

        //with options
        x1 = (Metadata::createFromBytes(m23,24));
        x2 = (Metadata::createFromString(s23,48));
        TS_ASSERT(x1.isEqual(x2));
        
    }
    //toString and sizeAsString
    void testXtrToString() {
        Metadata x1;
        char buf[255];
        char* r;
        
        x1 = (Metadata::createFromString(s1, strlen(s1)));
        r = x1.toString(buf, sizeof(buf));
        TS_ASSERT_EQUALS(strlen(r), strlen(s1));
        TS_ASSERT_SAME_DATA(r, s1, strlen(s1));
        
        x1 = (Metadata::createFromString(s3, strlen(s3)));
        r = x1.toString(buf, sizeof(buf));
        TS_ASSERT_EQUALS(strlen(r), strlen(s3));
        TS_ASSERT_SAME_DATA(r, s3, strlen(s3));
        
        x1 = (Metadata::createFromString(s7, strlen(s7)));
        r = x1.toString(buf, sizeof(buf));
        TS_ASSERT_EQUALS(strlen(r), strlen(s7));
        TS_ASSERT_SAME_DATA(r, s7, strlen(s7));
        
        x1 = (Metadata::createFromString(s23, strlen(s23)));
        r = x1.toString(buf, sizeof(buf));
        TS_ASSERT_EQUALS(strlen(r), strlen(s23));
        TS_ASSERT_SAME_DATA(r, s23, strlen(s23));
        
        //cerr << "Printing an Metadata string: " << x1.toString(buf, sizeof(buf)) << endl;
        //cerr << "It should be the same as      : " << s23 << endl;
    }

    //addOption
    void testXtrAddOption() {
        Option *o1,*o2,*o3,*o4;
        Metadata x1;
        u_int8_t b[256];    
        u_int8_t pack[256];
        size_t len;
        int i;
        
        u_int8_t m4_o1[15]     = {5,0,1,2,3,4,5,6,7,3,3,3,3,1,0};
        u_int8_t m4_o1o2[17]   = {5,0,1,2,3,4,5,6,7,3,3,3,3,3,0,1,0};
        u_int8_t m4_o1o2o3[21] = {5,0,1,2,3,4,5,6,7,3,3,3,3,7,0,1,0,2,2,0,1};
        u_int8_t m4_o1o3[19]     = {5,0,1,2,3,4,5,6,7,3,3,3,3,5,0,2,2,0,1};
        for (i = 0; i < 255; i++)
            b[i] = i;

        o1 = new OptionNop(); //nop
        o2 = new OptionAny(1,0,0); //type 1, length 0
        o3 = new OptionAny(2,2,b);
        o4 = new OptionAny(3,253,b);

        x1 = (Metadata::createFromBytes(m4, 13));

        //add to id w/o options
        TS_ASSERT_EQUALS(x1.addOption(*o1), XTR_SUCCESS) ;
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 1u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 1u);
        len = sizeof(pack); 
        x1.pack(pack, &len);
        TS_ASSERT_SAME_DATA(pack, m4_o1, sizeof(m4_o1));
    
        //add to id with other options
        TS_ASSERT_EQUALS(x1.addOption(*o2), XTR_SUCCESS);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 2u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 3u);
        len = sizeof(pack); 
        x1.pack(pack, &len);
        TS_ASSERT_SAME_DATA(pack, m4_o1o2, sizeof(m4_o1o2));

        TS_ASSERT_EQUALS(x1.addOption(*o3), XTR_SUCCESS);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 3u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 7u);
        len = sizeof(pack); 
        x1.pack(pack, &len);
        TS_ASSERT_SAME_DATA(pack, m4_o1o2o3, sizeof(m4_o1o2o3));
        
        //remove o2
        TS_ASSERT_EQUALS(x1.getOptions().removeOptionAt(1), XTR_SUCCESS);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 2u);
        TS_ASSERT_EQUALS(x1.getOptions().getLength(), 5u);
        len = sizeof(pack); 
        x1.pack(pack, &len);
        TS_ASSERT_SAME_DATA(pack, m4_o1o3, sizeof(m4_o1o3));

        //no space to add
        TS_ASSERT_EQUALS(x1.addOption(*o4), XTR_FAIL);
        len = sizeof(pack);
        x1.pack(pack, &len);
        TS_ASSERT_SAME_DATA(pack, m4_o1o3, sizeof(m4_o1o3));
        delete o1;
        delete o2;
        delete o3;
        delete o4;
        
    }

    void testXtrSetChainId() 
    {
        Metadata x1;
        u_int16_t id = 0x5455;

        TS_ASSERT_EQUALS(x1.getChainId(), (u_int16_t) 0);
        x1.setChainId(id);
        TS_ASSERT_EQUALS(x1.getChainId(), id);
        x1.newChainId();
        TS_ASSERT_DIFFERS(x1.getChainId(), id);
        id = x1.getChainId();
        x1.newChainId();
        TS_ASSERT_DIFFERS(x1.getChainId(), id);
    }

    void testXtrSetSeverity()
    {
        Metadata x1;
        u_int8_t severity = OptionSeverity::DEBUG;
        TS_ASSERT_EQUALS(x1.getSeverityThreshold(), OptionSeverity::_UNSET);
        //default is notice. This should break if we change the default
        TS_ASSERT_EQUALS(x1.setSeverityThreshold(severity), XTR_SUCCESS);
        TS_ASSERT_EQUALS(x1.getSeverityThreshold(), OptionSeverity::DEBUG);
        TS_ASSERT_EQUALS(x1.setSeverityThreshold(OptionSeverity::WARNING), XTR_SUCCESS);
        TS_ASSERT_EQUALS(x1.getSeverityThreshold(), OptionSeverity::WARNING);
        TS_ASSERT_EQUALS(x1.unsetSeverityThreshold(), XTR_SUCCESS);
        TS_ASSERT_EQUALS(x1.getSeverityThreshold(), OptionSeverity::_UNSET);
        TS_ASSERT_EQUALS(x1.setSeverityThreshold(OptionSeverity::NOTICE), XTR_SUCCESS);
        TS_ASSERT_EQUALS(x1.getSeverityThreshold(), OptionSeverity::NOTICE);
        TS_ASSERT_EQUALS(x1.unsetSeverityThreshold(), XTR_SUCCESS);
        TS_ASSERT_EQUALS(x1.getSeverityThreshold(), OptionSeverity::_UNSET);
        //the call should be idempotent
        TS_ASSERT_EQUALS(x1.unsetSeverityThreshold(), XTR_SUCCESS);
        TS_ASSERT_EQUALS(x1.getSeverityThreshold(), OptionSeverity::_UNSET);

        //now we test the case in which multiple severity options are present
        //this can't be constructed by the regular calls, but might come in 
        //metadata constructed elsewhere... Remember Postel's robustness principle:
        //be liberal in what you accept, conservative on what you send (or generate)

        Option *o1, *o2;

        TS_ASSERT_EQUALS(x1.setSeverityThreshold(OptionSeverity::DEBUG), XTR_SUCCESS);
        //Now add another option just for kicks
        o1 = new OptionNop(); //nop
        TS_ASSERT_EQUALS(x1.addOption(*o1), XTR_SUCCESS) ;
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 2u);
        o2 = new OptionSeverity(OptionSeverity::NOTICE);
        TS_ASSERT_EQUALS(x1.addOption(*o2), XTR_SUCCESS);
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 3u);
        //getSeverityThreshold should return the first one
        TS_ASSERT_EQUALS(x1.getSeverityThreshold(), OptionSeverity::DEBUG);
        TS_ASSERT_EQUALS(x1.unsetSeverityThreshold(), XTR_SUCCESS);
        TS_ASSERT_EQUALS(x1.getSeverityThreshold(), OptionSeverity::_UNSET);
        //the previous call should have removed both options, leaving only the
        //nop options alone.
        TS_ASSERT_EQUALS(x1.getOptions().getCount(), 1u);
    }

    void testXtrEvenCtx() 
    { 
        Event model;
    
        Metadata x1;
        Metadata x2;
        Metadata x3; //different chain id
        
        model.addInfo("This-key","comes from template context");

        Event e1(model);
        e1.addInfo("Agent","TestXtr");

        x1.setRandomTaskId();
        x1.setRandomOpId();
        x1.setChainId(0x51);
        x1.setSeverityThreshold(OptionSeverity::DEBUG);

        e1.addEdge(x1); //this will set the severity and the chainId of the 
                        // event. Will also set the opId length of
                        // the event.

        x1.setRandomOpId(); 
        e1.addEdge(x1,EventEdge::UP);
    
        x2.setRandomTaskId();
        x2.setRandomOpId(); 

        x3.setTaskId(x1.getTaskId());
        x3.setRandomOpId(8); //will not change the event opId length
        x3.setChainId(0x2344);

        //fails because of different task id
        TS_ASSERT_EQUALS(e1.addEdge(x2,EventEdge::UP),XTR_FAIL);
        //succeeds
        TS_ASSERT_EQUALS(e1.addEdge(x3,EventEdge::NEXT),XTR_SUCCESS);


        //get metadata
        Metadata x4 = e1.getMetadata();
        TS_ASSERT(x4.getSeverityThreshold() == OptionSeverity::DEBUG);
        TS_ASSERT(x4.getTaskId().isEqual(x1.getTaskId()));
        TS_ASSERT_EQUALS(x4.getChainId(),x1.getChainId());
        TS_ASSERT_EQUALS(x4.getOpId().getLength(), x1.getOpId().getLength());
        e1.fork();
        x4 = e1.getMetadata();
        TS_ASSERT_DIFFERS(x4.getChainId(),x3.getChainId());
        TS_ASSERT_DIFFERS(x4.getChainId(),x1.getChainId());
        x4 = e1.getMetadata(0); //by index
        TS_ASSERT_EQUALS(x4.getChainId(),x1.getChainId());
        x4 = e1.getMetadata(1); //by index
        TS_ASSERT_DIFFERS(x4.getChainId(),x1.getChainId());
        x4 = e1.getMetadata(3); //inexistent index gets 0
        TS_ASSERT_EQUALS(x4.getChainId(),x1.getChainId());

        cout << endl << e1.getReport();
    }

    void testReportCtxSeverity() 
    {
        TS_ASSERT_EQUALS( Reporter::willReport(OptionSeverity::EMERG), XTR_FAIL);
        Reporter::init();
        //default severity threshold is _DEFAULT (=NOTICE)
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::DEBUG)   , XTR_FAIL_SEVERITY);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::INFO)    , XTR_FAIL_SEVERITY);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::NOTICE)  , XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::WARNING) , XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::ERROR)   , XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::CRITICAL), XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::ALERT)   , XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::EMERG)   , XTR_SUCCESS);

        //now test override from metadata threshold, for, say, ERROR.
        // we shouldn't report anything below error with the override
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::DEBUG   ,OptionSeverity::ERROR), XTR_FAIL_SEVERITY);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::INFO    ,OptionSeverity::ERROR), XTR_FAIL_SEVERITY);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::NOTICE  ,OptionSeverity::ERROR), XTR_FAIL_SEVERITY);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::WARNING ,OptionSeverity::ERROR), XTR_FAIL_SEVERITY);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::ERROR   ,OptionSeverity::ERROR), XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::CRITICAL,OptionSeverity::ERROR), XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::ALERT   ,OptionSeverity::ERROR), XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::EMERG   ,OptionSeverity::ERROR), XTR_SUCCESS);
 
        Reporter::setSeverityThreshold(OptionSeverity::_ALL);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::DEBUG)   , XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::INFO)    , XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::NOTICE)  , XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::WARNING) , XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::ERROR)   , XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::CRITICAL), XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::ALERT)   , XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::EMERG)   , XTR_SUCCESS);

        Reporter::setSeverityThreshold(OptionSeverity::_NONE);
        TS_ASSERT_EQUALS( Reporter::willReport(OptionSeverity::DEBUG)   , XTR_FAIL_SEVERITY);
        TS_ASSERT_EQUALS( Reporter::willReport(OptionSeverity::INFO)    , XTR_FAIL_SEVERITY);
        TS_ASSERT_EQUALS( Reporter::willReport(OptionSeverity::NOTICE)  , XTR_FAIL_SEVERITY);
        TS_ASSERT_EQUALS( Reporter::willReport(OptionSeverity::WARNING) , XTR_FAIL_SEVERITY);
        TS_ASSERT_EQUALS( Reporter::willReport(OptionSeverity::ERROR)   , XTR_FAIL_SEVERITY);
        TS_ASSERT_EQUALS( Reporter::willReport(OptionSeverity::CRITICAL), XTR_FAIL_SEVERITY);
        TS_ASSERT_EQUALS( Reporter::willReport(OptionSeverity::ALERT)   , XTR_FAIL_SEVERITY);
        TS_ASSERT_EQUALS( Reporter::willReport(OptionSeverity::EMERG)   , XTR_FAIL_SEVERITY);

        //Again test the override. This should be a common case, _NONE
        //is the reporter threshold, and a metadata comes along with
        //some other threshold.
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::DEBUG   ,OptionSeverity::INFO), XTR_FAIL_SEVERITY);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::INFO    ,OptionSeverity::INFO), XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::NOTICE  ,OptionSeverity::INFO), XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::WARNING ,OptionSeverity::INFO), XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::ERROR   ,OptionSeverity::INFO), XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::CRITICAL,OptionSeverity::INFO), XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::ALERT   ,OptionSeverity::INFO), XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::EMERG   ,OptionSeverity::INFO), XTR_SUCCESS);
    
        Reporter::setSeverityThreshold(OptionSeverity::WARNING);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::DEBUG)   , XTR_FAIL_SEVERITY);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::INFO)    , XTR_FAIL_SEVERITY);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::NOTICE)  , XTR_FAIL_SEVERITY);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::WARNING) , XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::ERROR)   , XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::CRITICAL), XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::ALERT)   , XTR_SUCCESS);
        TS_ASSERT_EQUALS(  Reporter::willReport(OptionSeverity::EMERG)   , XTR_SUCCESS);

    }

    void testXtrContext() 
    {
        //disable actual reporting
        Reporter::setSeverityThreshold(OptionSeverity::_NONE);

        //test initial value
        TS_ASSERT(! Context::getContext().isValid());
        Metadata xtr;
        xtr.setRandomTaskId();
        //set some value
        Context::setContext(xtr);
        //make sure it is the same
        TS_ASSERT(xtr.isEqual(Context::getContext()));
        //unset
        Context::unsetContext();
        TS_ASSERT(! Context::getContext().isValid());

        Context::setContext(xtr);
    
        Context::logEvent("Hello","World");
        
        //make sure the context was advanced
        TS_ASSERT(! xtr.isEqual(Context::getContext()));
        TS_ASSERT(xtr.getTaskId().isEqual(Context::getContext().getTaskId()));
        TS_ASSERT(! xtr.getOpId().isEqual(Context::getContext().getOpId()));
        
        auto_ptr<Event> xte = Context::createEvent("Hello","World");
        TS_ASSERT(Context::getContext().isEqual(xte->getMetadata()));
            
        cout << endl << xte->getReport();
    }
};   
