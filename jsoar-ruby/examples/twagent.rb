# Install jruby 1.2.0 or newer and put it on the path
# $ gem install twitter   (0.6.8) 
# $ gem install jruby-openssl (0.4)
# set JSOAR_HOME
# $ jruby twagent.rb user pass soar-file

require 'rubygems'
require 'twitter'
require 'rsoar'
require 'set'

class Twagent

  def initialize(user, pass, file)
    
    @known_tweets = Set.new
    @users = {}
    @last_tweet = nil
    @api_calls = 0
    @httpauth = Twitter::HTTPAuth.new(user, pass)
    @twitter = Twitter::Base.new(@httpauth)
    
    @agent = RSoar::RAgent.new do |a|
      a.debugger_provider = org.jsoar.debugger.JSoarDebugger.new_debugger_provider
    
      a.output_link.when :update do |v|
        begin
          a.print "\nenv: Sending tweet #{v[:text]}\n"
          @twitter.update v.text 
          'complete'
        rescue Twitter::General => e
          a.print "env: ERROR: #{e}"
          'error'
        end
      end
      
      a.output_link.when :direct do |v|
        begin
          a.print "\nenv: Sending direct message to #{v.user.screen_name}: #{v.text}\n"
          @twitter.direct_message_create v.user[:id], v.text 
          'complete'
        rescue Twitter::General => e
          a.print "env: ERROR: #{e}"
          'error'
        end
      end
      
      a.output_link.when :follow do |v|
        begin
          a.input.atomic do
            a.print "\nenv: following #{v.user.screen_name}\n"
            @twitter.friendship_create v.user[:id], true 
          end
          'complete'
        rescue Twitter::General => e
          a.print "env: ERROR: #{e}"
          'error'
        end
      end
      
      a.output_link.when :unfollow do |v|
        begin
          a.input.atomic do
            a.print "\nenv: unfollowing #{v.user.screen_name}\n"
            @twitter.friendship_destroy v.user[:id] 
          end
          'complete'
        rescue Twitter::General => e
          a.print "env: ERROR: #{e}"
          'error'
        end
      end
      
      a.input_link.^ :api_calls, @api_calls
      @tweet_root = a.input_link.+ :tweets
      @users_root = a.input_link.+ :users
      @followers_root = a.input_link.+ :followers
      @friends_root = a.input_link.+ :friends
      a.debug
    end
    
    if file
      @agent.source file
    end
  end

  def copy_attrs(from, to, attrs)
    attrs.each do |a|
      v = from.send(a)
      to.^(a, from.send(a)) if v
    end
  end

  def get_followers() get_user_list @followers_root, :followers end
  def get_friends() get_user_list @friends_root, :friends end
  
  def get_user_list(root, method)
    @twitter.send(method).each do |f|
      uid = create_user_input f
      root.^ f.screen_name, uid
    end
  end

  def create_user_input(user)
    uid = @users[user[:id]]
    if uid.nil?
      uid = @users_root.+ :user
      copy_attrs user, uid, [:name, :id, :screen_name, :location, :followers_count, :friends_count]
      @users[user[:id]] = uid
    end
    uid
  end
  
  def create_tweet_input(tweet)
    @tweet_root.+ tweet[:id] do |tid|
      copy_attrs tweet, tid, [:id, :text, :source]
      
      tid.+ :user, create_user_input(tweet.user)
      
      if !@last_tweet.nil?
        tid.previous = @last_tweet
      end
    end
  end
  
  def update
    @agent.input.atomic do |il|
      update_tweets
      
      il.root.^ :api_calls, (@api_calls += 1)
    end
  end
  
  def update_tweets
    @twitter.friends_timeline.each do |tweet| 
      if !@known_tweets.include?(tweet[:id])
        @agent.print "\nenv: New tweet #{tweet[:id]}\n"
        @last_tweet = create_tweet_input tweet
        @known_tweets.add tweet[:id]
      end
    end  
  end

end

org.jsoar.debugger.JSoarDebugger::initialize_look_and_feel

user, pass, file = ARGV
twagent = Twagent.new user, pass, file
sleep 2.0

twagent.get_followers
twagent.get_friends
while true
  twagent.update
  sleep 60.0
end
